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
            val merged = local.loadFields()
            val remoteById = mutableMapOf<String, SavedField>()
            for (doc in snapshot.documents) {
                val raw = doc.getString("payload") ?: continue
                val remote = try { FieldStorage.fromJson(JSONObject(raw)) } catch (_: Exception) { null } ?: continue
                if (remote.id == doc.id) remoteById[doc.id] = remote
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
                        // Both changed, or this phone has no verified baseline.
                        // Keep the local original; preserve remote edits as a stable copy.
                        val copyId = UUID.nameUUIDFromBytes(
                            (uid + ":" + id + ":" + remoteJson).toByteArray(Charsets.UTF_8)
                        ).toString()
                        if (merged.none { it.id == copyId } && !remoteById.containsKey(copyId)) {
                            val copy = remote.copy(id = copyId, name = remote.name + " (cloud copy)")
                            merged.add(copy)
                            uploads[copyId] = copy
                            conflicts++
                        }
                        // The original cloud record stays unchanged for now.
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
            if (uploads.isEmpty()) {
                recordBaselines()
                finish("Sync complete. Restored $recovered field(s); preserved $conflicts conflict copy/copies.", done)
                return@addOnSuccessListener
            }
            if (uploads.size > 450) {
                finish("Local fields saved, but too many cloud updates for one batch. No cloud changes made.", done)
                return@addOnSuccessListener
            }
            val batch = db.batch()
            for ((id, field) in uploads) {
                batch.set(collection.document(id), mapOf("payload" to FieldStorage.toJson(field).toString()))
            }
            batch.commit().addOnSuccessListener {
                recordBaselines()
                finish("Sync complete. Restored $recovered, preserved $conflicts conflict copy/copies, uploaded ${uploads.size}.", done)
            }.addOnFailureListener {
                finish("Local fields preserved; cloud upload failed: ${it.localizedMessage}", done)
            }
        }.addOnFailureListener {
            finish("Sync unavailable: ${it.localizedMessage}. Local fields remain unchanged.", done)
        }
    }
}
