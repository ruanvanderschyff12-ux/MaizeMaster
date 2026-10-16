package com.maizemaster.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.util.UUID

/**
 * Non-destructive manual two-way sync.
 * A per-account baseline identifies which side changed since the last successful sync.
 * Concurrent edits are preserved as copies rather than silently overwritten.
 * Cloud deletion is deliberately not supported in this phase.
 */
class FieldCloudSync(private val context: Context) {
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

    private val syncState = context.getSharedPreferences("maizemaster_sync_state", Context.MODE_PRIVATE)
    private fun finish(message: String, done: (String) -> Unit) {
        syncState.edit().putString("status", message)
            .putLong("updated_at", System.currentTimeMillis()).apply()
        done(message)
    }

    fun sync(done: (String) -> Unit) {
        syncState.edit().putString("status", "Syncing fields...").apply()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish("Sign in before syncing.", done)
            return
        }
        val collection = db.collection("farmers").document(uid).collection("fields")
        collection.get().addOnSuccessListener { snapshot ->
            val deletedIds = local.pendingDeletedIds()
            val merged = local.loadFields().filterNot { it.id in deletedIds }.toMutableList()
            val remoteById = mutableMapOf<String, SavedField>()
            for (doc in snapshot.documents) {
                val raw = doc.getString("payload") ?: continue
                val remote = try { FieldStorage.fromJson(JSONObject(raw)) } catch (_: Exception) { null } ?: continue
                if (remote.id == doc.id && doc.id !in deletedIds) remoteById[doc.id] = remote
            }

            val uploads = linkedMapOf<String, SavedField>()
            var recovered = 0
            var conflicts = 0
            val localById = merged.associateBy { it.id }
            for ((id, remote) in remoteById) {
                val current = localById[id]
                if (current == null) {
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
                        if (merged.none { it.id == copy.id } && !remoteById.containsKey(copy.id)) {
                            merged.add(copy)
                            uploads[copy.id] = copy
                            conflicts++
                        }
                        merged[merged.indexOfFirst { it.id == id }] = remote
                        recovered++
                    }
                }
            }
            for (field in merged) {
                if (!remoteById.containsKey(field.id) && !uploads.containsKey(field.id)) {
                    uploads[field.id] = field
                }
            }
            if (!local.saveFields(merged)) {
                finish("Could not save local fields. Cloud was not changed.", done)
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
            if (uploads.isEmpty() && deletedIds.isEmpty()) {
                recordBaselines()
                finish("Sync complete. Restored $recovered field(s); preserved $conflicts conflict copy/copies.", done)
                return@addOnSuccessListener
            }
            if (uploads.size + deletedIds.size > 450) {
                finish("Local fields saved, but too many cloud updates for one batch. No cloud changes made.", done)
                return@addOnSuccessListener
            }
            // Compare cloud values inside a Firestore transaction before any write.
            // A second phone can upload between our initial read and commit.
            val ordered = uploads.toList()
            db.runTransaction { tx ->
                val currentDocs = ordered.map { (id, _) ->
                    tx.get(collection.document(id))
                }
                val deletionDocs = deletedIds.map { id -> id to tx.get(collection.document(id)) }
                var raced = 0
                for (index in ordered.indices) {
                    val (id, field) = ordered[index]
                    val actual = currentDocs[index].getString("payload")
                    val expected = remoteById[id]?.let { FieldStorage.toJson(it).toString() }
                    if (actual == expected) {
                        tx.set(collection.document(id), mapOf("payload" to FieldStorage.toJson(field).toString()))
                    } else {
                        // Do not overwrite a concurrent cloud edit.
                        // Store our local edit as a distinct immutable snapshot.
                        val copy = copyOf(field, uid)
                        tx.set(collection.document(copy.id), mapOf("payload" to FieldStorage.toJson(copy).toString()))
                        raced++
                    }
                }
                for ((id, doc) in deletionDocs) {
                    val baseline = baselines.getString("$uid:$id", null)
                    val payload = doc.getString("payload")
                    if (payload != null && baseline != null && payload != baseline) {
                        val changed = try { FieldStorage.fromJson(JSONObject(payload)) } catch (_: Exception) { null }
                        if (changed != null) {
                            val copy = copyOf(changed, uid)
                            tx.set(collection.document(copy.id), mapOf("payload" to FieldStorage.toJson(copy).toString()))
                        }
                    }
                    tx.delete(collection.document(id))
                }
                raced
            }.addOnSuccessListener { raced ->
                if (raced == 0) recordBaselines()
                for (id in deletedIds) local.clearPendingDeletion(id)
                finish("Sync complete. Preserved $conflicts conflict copies; $raced concurrent edit(s) saved as copies. Reopen to refresh.", done)
            }.addOnFailureListener {
                finish("Local fields preserved; cloud upload failed: ${it.localizedMessage}", done)
            }
        }.addOnFailureListener {
            finish("Sync unavailable: ${it.localizedMessage}. Local fields remain unchanged.", done)
        }
    }
}
