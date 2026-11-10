package com.maizemaster.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Non-destructive manual two-way sync.
 * A per-account baseline identifies which side changed since the last successful sync.
 * Concurrent edits are preserved as copies rather than silently overwritten.
 * Cloud deletion is deliberately not supported in this phase.
 */
class FieldCloudSync(context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val local = FieldStorage(context)
    private val baselines = context.getSharedPreferences("maizemaster_sync_baselines", Context.MODE_PRIVATE)

    private fun copyOf(field: SavedField, uid: String): SavedField {
        val raw = FieldStorage.toJson(field).toString()
        val copyId = UUID.nameUUIDFromBytes(
            (uid + ":" + field.id + ":" + raw).toByteArray(Charsets.UTF_8)
        ).toString()
        return field.copy(id = copyId, name = field.name + " (conflict copy)")
    }

    fun sync(done: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            done("Sign in before syncing.")
            return
        }
        if (!syncRunning.compareAndSet(false, true)) {
            done("A field sync is already running.")
            return
        }
        fun finish(message: String) {
            syncRunning.set(false)
            done(message)
        }

        val collection = db.collection("farmers").document(uid).collection("fields")
        collection.get(Source.SERVER).addOnSuccessListener { snapshot ->
            if (auth.currentUser?.uid != uid) {
                finish("Account changed. Start sync again from the current account.")
                return@addOnSuccessListener
            }
            val merged = local.loadFields()
            val locallyDeletedIds = local.locallyDeletedFieldIds()
            val remoteById = mutableMapOf<String, SavedField>()
            val rawById = snapshot.documents.associate { it.id to (it.get("payload") as? String) }
            for ((id, raw) in rawById) {
                if (raw == null) continue
                val remote = try { FieldStorage.fromJson(JSONObject(raw)) } catch (_: Exception) { null } ?: continue
                if (remote.id == id) remoteById[id] = remote
            }

            val uploads = linkedMapOf<String, SavedField>()
            var recovered = 0
            var conflicts = 0
            val localById = merged.associateBy { it.id }
            for ((id, remote) in remoteById) {
                val current = localById[id]
                if (current == null) {
                    if (id in locallyDeletedIds) continue
                    merged.add(remote)
                    recovered++
                    continue
                }
                val localJson = FieldStorage.toJson(current).toString()
                val remoteJson = FieldStorage.toJson(remote).toString()
                if (localJson == remoteJson) continue

                val previous = baselines.getString("$uid:$id", null)
                when {
                    previous != null && localJson == previous -> {
                        // Only the cloud version changed.
                        merged[merged.indexOfFirst { it.id == id }] = remote
                        recovered++
                    }
                    previous != null && remoteJson == previous -> {
                        // Only this phone changed.
                        uploads[id] = current
                    }
                    else -> {
                        // Preserve the local edit under a new stable ID.
                        // Keep the remote version at the original ID.
                        val copy = copyOf(current, uid)
                        val existing = merged.firstOrNull { it.id == copy.id } ?: remoteById[copy.id]
                        if (existing == null) {
                            merged.add(copy)
                            conflicts++
                        } else if (FieldStorage.toJson(existing).toString() != FieldStorage.toJson(copy).toString()) {
                            merged.add(copy.copy(id = UUID.randomUUID().toString()))
                            conflicts++
                        }
                        merged[merged.indexOfFirst { it.id == id }] = remote
                        recovered++
                    }
                }
            }
            for (field in merged) {
                if (field.id !in rawById && field.id !in uploads) {
                    uploads[field.id] = field
                }
            }
            if (recovered > 0 && !local.saveFields(merged)) {
                finish("Could not save local fields. Cloud was not changed.")
                return@addOnSuccessListener
            }
            fun recordBaselines() {
                val editor = baselines.edit()
                for (field in merged) {
                    val uploaded = uploads[field.id]
                    val cloud = remoteById[field.id]
                    val synced = uploaded ?: cloud
                    if (synced != null && FieldStorage.toJson(field).toString() == FieldStorage.toJson(synced).toString()) {
                        editor.putString("$uid:${field.id}", FieldStorage.toJson(field).toString())
                    }
                }
                editor.apply()
            }
            if (uploads.isEmpty()) {
                recordBaselines()
                finish("Sync complete. Restored $recovered field(s); preserved $conflicts conflict copy/copies.")
                return@addOnSuccessListener
            }
            if (uploads.size > 450) {
                finish("Local fields saved, but too many cloud updates for one batch. No cloud changes made.")
                return@addOnSuccessListener
            }
            db.runTransaction { transaction ->
                for (id in uploads.keys) {
                    val actual = transaction.get(collection.document(id))
                    if (actual.exists() != rawById.containsKey(id) || (actual.get("payload") as? String) != rawById[id]) {
                        throw FirebaseFirestoreException(
                            "A field changed on another phone. Sync again to include that edit.",
                            FirebaseFirestoreException.Code.FAILED_PRECONDITION
                        )
                    }
                }
                for ((id, field) in uploads) {
                    transaction.set(collection.document(id), mapOf("payload" to FieldStorage.toJson(field).toString()))
                }
            }.addOnSuccessListener {
                recordBaselines()
                finish("Sync complete. Updated ${uploads.size} cloud field(s), received $recovered field(s), and kept $conflicts conflict copy/copies.")
            }.addOnFailureListener {
                finish("Local fields preserved. Cloud sync could not finish: ${it.localizedMessage}")
            }
        }.addOnFailureListener {
            finish("Sync unavailable: ${it.localizedMessage}. Local fields remain unchanged.")
        }
    }

    companion object {
        private val syncRunning = AtomicBoolean()

        private fun settings(context: Context) =
            context.getSharedPreferences("maizemaster_auto_field_sync", Context.MODE_PRIVATE)

        fun isAutomaticEnabled(context: Context, uid: String): Boolean =
            settings(context).getBoolean(uid, false)

        fun setAutomaticEnabled(context: Context, uid: String, enabled: Boolean) {
            settings(context).edit().putBoolean(uid, enabled).apply()
        }
    }
}
