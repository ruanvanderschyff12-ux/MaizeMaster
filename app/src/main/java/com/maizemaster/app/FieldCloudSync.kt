package com.maizemaster.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.util.UUID

/**
 * Explicit, non-destructive cloud merge. Never deletes cloud or local fields.
 * Conflicting field versions are preserved as separate copies.
 */
class FieldCloudSync(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val local = FieldStorage(context)

    fun sync(done: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            done("Sign in before syncing.")
            return
        }
        val collection = db.collection("farmers").document(uid).collection("fields")
        val initial = local.loadFields()
        collection.get().addOnSuccessListener { snapshot ->
            val merged = local.loadFields()
            val uploads = mutableListOf<Pair<String, Map<String, Any>>>()
            var recovered = 0
            var conflicts = 0
            for (doc in snapshot.documents) {
                val raw = doc.getString("payload") ?: continue
                val remote = try { FieldStorage.fromJson(JSONObject(raw)) } catch (_: Exception) { null } ?: continue
                val index = merged.indexOfFirst { it.id == remote.id }
                if (index == -1) {
                    merged.add(remote)
                    recovered++
                } else if (FieldStorage.toJson(merged[index]).toString() != FieldStorage.toJson(remote).toString()) {
                    val copy = remote.copy(id = UUID.nameUUIDFromBytes((remote.id + raw).toByteArray(Charsets.UTF_8)).toString(), name = remote.name + " (cloud copy)")
                    if (merged.none { it.id == copy.id }) {
                        merged.add(copy)
                        uploads.add(copy.id to mapOf("payload" to FieldStorage.toJson(copy).toString()))
                        conflicts++
                    }
                }
            }
            if (!local.saveFields(merged)) {
                done("Could not save local fields. Cloud was not changed.")
                return@addOnSuccessListener
            }
            val existingIds = snapshot.documents.map { it.id }.toSet()
            for (field in merged) {
                if (field.id !in existingIds) uploads.add(field.id to mapOf("payload" to FieldStorage.toJson(field).toString()))
            }
            if (uploads.isEmpty()) {
                done("Sync complete. Restored $recovered field(s); kept $conflicts conflicting copy/copies.")
                return@addOnSuccessListener
            }
            val batch = db.batch()
            for ((id, data) in uploads.distinctBy { it.first }) batch.set(collection.document(id), data)
            batch.commit().addOnSuccessListener {
                done("Sync complete. Restored $recovered field(s), preserved $conflicts conflict copy/copies and backed up ${uploads.size} field(s).")
            }.addOnFailureListener {
                done("Local fields preserved; cloud backup failed: ${it.localizedMessage}")
            }
        }.addOnFailureListener {
            done("Sync unavailable: ${it.localizedMessage}. Local fields remain unchanged.")
        }
    }
}
