package com.maizemaster.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FieldCloudBackup {
    private const val OWNER_KEY = "cloud_backup_owner_uid"

    fun syncAfterLocalSave(prefs: SharedPreferences, fields: List<SavedField>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (prefs.getString(OWNER_KEY, null) != uid) return
        val values = fields.map { fieldToMap(it) }
        FirebaseFirestore.getInstance().collection("farmers").document(uid)
            .collection("backups").document("fields").set(mapOf("fields" to values))
            .addOnFailureListener { android.util.Log.e("MaizeMaster", "Cloud sync failed", it) }
    }

    private fun fieldToMap(f: SavedField): Map<String, Any> = mapOf(
        "id" to f.id, "name" to f.name, "shape" to f.shape,
        "width" to f.width, "length" to f.length,
        "rowSpacing" to f.rowSpacing, "plantSpacing" to f.plantSpacing,
        "maizeVariety" to f.maizeVariety, "expectedYield" to f.expectedYield,
        "harvestedHectares" to f.harvestedHectares, "harvestedTons" to f.harvestedTons,
        "plantingDate" to f.plantingDate, "maturityDays" to f.maturityDays,
        "growthStage" to f.growthStage, "growthNotes" to f.growthNotes,
        "growthObservedDate" to f.growthObservedDate
    )

    fun backup(activity: Activity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(activity, "Sign in first", Toast.LENGTH_LONG).show()
            return
        }
        val fields = FieldStorage(activity).loadFields().map { fieldToMap(it) }
        FirebaseFirestore.getInstance()
            .collection("farmers").document(uid)
            .collection("backups").document("fields")
            .set(mapOf("fields" to fields))
            .addOnSuccessListener {
                activity.getSharedPreferences("maizemaster_fields", Context.MODE_PRIVATE).edit().putString(OWNER_KEY, uid).apply()
                Toast.makeText(activity, "Fields backed up. Auto sync enabled.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Cloud backup failed", Toast.LENGTH_LONG).show()
            }
    }
    fun restore(activity: Activity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(activity, "Sign in first", Toast.LENGTH_LONG).show()
            return
        }
        FirebaseFirestore.getInstance().collection("farmers").document(uid)
            .collection("backups").document("fields").get()
            .addOnSuccessListener { snapshot ->
                val raw = snapshot.get("fields") as? List<*>
                if (raw == null) {
                    Toast.makeText(activity, "No cloud backup found", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val fields = try {
                    raw.map { entry ->
                        val m = entry as Map<*, *>
                        fun s(key: String) = m[key] as? String ?: ""
                        fun d(key: String) = (m[key] as? Number)?.toDouble() ?: 0.0
                        SavedField(
                            id = s("id").also { require(it.isNotBlank()) },
                            name = s("name"), shape = s("shape"),
                            width = d("width"), length = d("length"),
                            rowSpacing = d("rowSpacing"), plantSpacing = d("plantSpacing"),
                            maizeVariety = s("maizeVariety"), expectedYield = d("expectedYield"),
                            harvestedHectares = d("harvestedHectares"), harvestedTons = d("harvestedTons"),
                            plantingDate = s("plantingDate"),
                            maturityDays = (m["maturityDays"] as? Number)?.toInt() ?: 0,
                            growthStage = s("growthStage"), growthNotes = s("growthNotes"),
                            growthObservedDate = s("growthObservedDate")
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(activity, "Invalid cloud backup", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                android.app.AlertDialog.Builder(activity)
                    .setTitle("Restore fields?")
                    .setMessage("Replace the fields on this phone with ${fields.size} fields from your account? Back up local fields first if needed.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Restore") { _, _ ->
                        val saved = FieldStorage(activity).saveFields(fields)
                        if (saved) activity.getSharedPreferences("maizemaster_fields", Context.MODE_PRIVATE).edit().putString(OWNER_KEY, uid).apply()
                        Toast.makeText(activity,
                            if (saved) "Fields restored. Reopen My Fields to refresh." else "Restore failed",
                            Toast.LENGTH_LONG).show()
                    }.show()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Could not download backup", Toast.LENGTH_LONG).show()
            }
    }
}
