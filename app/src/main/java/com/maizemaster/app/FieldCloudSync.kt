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
 * Two-way sync with per-account baselines and recoverable edit conflicts.
 * A deletion marker occupies the original cloud ID so offline phones cannot resurrect it.
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
        val priorOwners = baselines.all.keys.map { it.substringBefore(':') }.toSet()
        val owner = local.syncOwner()
        if ((owner != null && owner != uid) ||
            (owner == null && priorOwners.any { it != uid })) {
            done("These saved fields are linked to another account. Sign in with the original account to sync them.")
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
            if (owner == null && !local.bindSyncOwner(uid)) {
                finish("Could not link saved fields to this account.")
                return@addOnSuccessListener
            }

            val merged = local.loadFields()
            val locallyDeletedIds = local.locallyDeletedFieldIds()
            val pendingDeletions = local.pendingCloudDeletions()
            val remoteById = mutableMapOf<String, SavedField>()
            val deletedInCloud = mutableSetOf<String>()
            val rawById = snapshot.documents.associate { it.id to (it.get("payload") as? String) }
            for ((id, raw) in rawById) {
                if (raw == null) continue
                val payload = try { JSONObject(raw) } catch (_: Exception) { continue }
                if (payload.optBoolean("deleted") && payload.optString("id") == id) {
                    deletedInCloud.add(id)
                    continue
                }
                val remote = try { FieldStorage.fromJson(payload) } catch (_: Exception) { continue }
                if (remote.id == id) remoteById[id] = remote
            }

            val uploads = linkedMapOf<String, String>()
            val publishedDeletions = mutableSetOf<String>()
            var received = 0
            var deleted = 0
            var conflicts = 0

            fun preserveConflict(field: SavedField) {
                val copy = copyOf(field, uid)
                if (copy.id in deletedInCloud || copy.id in locallyDeletedIds) return
                val existing = merged.firstOrNull { it.id == copy.id } ?: remoteById[copy.id]
                if (existing == null && copy.id !in rawById) {
                    merged.add(copy)
                    conflicts++
                } else if (existing != copy) {
                    merged.add(copy.copy(id = UUID.randomUUID().toString()))
                    conflicts++
                }
            }

            for (id in deletedInCloud) {
                val field = merged.firstOrNull { it.id == id } ?: continue
                val previous = baselines.getString("$uid:$id", null)
                if (FieldStorage.toJson(field).toString() != previous) preserveConflict(field)
                merged.remove(field)
                deleted++
            }
            for ((id, removedVersion) in pendingDeletions) {
                if (id in deletedInCloud) continue
                val remote = remoteById[id]
                if (remote == null && id in rawById) continue
                if (remote != null) {
                    val current = FieldStorage.toJson(remote).toString()
                    val previous = baselines.getString("$uid:$id", null)
                    if (current != removedVersion && current != previous) preserveConflict(remote)
                }
                uploads[id] = JSONObject().put("id", id).put("deleted", true).toString()
                publishedDeletions.add(id)
            }

            val localById = merged.associateBy { it.id }
            for ((id, remote) in remoteById) {
                if (id in pendingDeletions) continue
                val current = localById[id]
                if (current == null) {
                    if (id in locallyDeletedIds) continue
                    merged.add(remote)
                    received++
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
                        received++
                    }
                    previous != null && remoteJson == previous -> {
                        // Only this phone changed.
                        uploads[id] = localJson
                    }
                    else -> {
                        preserveConflict(current)
                        merged[merged.indexOfFirst { it.id == id }] = remote
                        received++
                    }
                }
            }
            for (field in merged) {
                if (field.id !in rawById && field.id !in uploads) {
                    uploads[field.id] = FieldStorage.toJson(field).toString()
                }
            }
            if ((received > 0 || deleted > 0 || conflicts > 0) && !local.saveFields(merged)) {
                finish("Could not save local fields. Cloud was not changed.")
                return@addOnSuccessListener
            }
            fun complete() {
                val markers = deletedInCloud + publishedDeletions
                val editor = baselines.edit()
                for (id in markers) editor.remove("$uid:$id")
                for (field in merged) {
                    val json = FieldStorage.toJson(field).toString()
                    val synced = uploads[field.id] ?: remoteById[field.id]?.let {
                        FieldStorage.toJson(it).toString()
                    }
                    if (json == synced) editor.putString("$uid:${field.id}", json)
                }
                editor.apply()
                local.acknowledgeCloudDeletions(markers)
                finish(
                    "Sync complete. Updated ${uploads.size - publishedDeletions.size} cloud field(s), " +
                        "received $received, synced ${deleted + publishedDeletions.size} deletion(s), " +
                        "and kept $conflicts conflict copy/copies."
                )
            }
            if (uploads.isEmpty()) {
                complete()
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
                for ((id, payload) in uploads) {
                    transaction.set(collection.document(id), mapOf("payload" to payload))
                }
            }.addOnSuccessListener {
                complete()
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
