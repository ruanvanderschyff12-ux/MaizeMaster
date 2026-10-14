package com.maizemaster.app

import android.app.Activity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FieldCloudBackup {
    fun backup(activity: Activity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(activity, "Sign in first", Toast.LENGTH_LONG).show()
            return
        }
        val fields = FieldStorage(activity).loadFields().map { field ->
            mapOf(
                "id" to field.id,
                "name" to field.name,
                "shape" to field.shape,
                "width" to field.width,
                "length" to field.length,
                "rowSpacing" to field.rowSpacing,
                "plantSpacing" to field.plantSpacing,
                "maizeVariety" to field.maizeVariety,
                "expectedYield" to field.expectedYield,
                "harvestedHectares" to field.harvestedHectares,
                "harvestedTons" to field.harvestedTons,
                "plantingDate" to field.plantingDate,
                "maturityDays" to field.maturityDays,
                "growthStage" to field.growthStage,
                "growthNotes" to field.growthNotes,
                "growthObservedDate" to field.growthObservedDate
            )
        }
        FirebaseFirestore.getInstance()
            .collection("farmers").document(uid)
            .collection("backups").document("fields")
            .set(mapOf("fields" to fields))
            .addOnSuccessListener {
                Toast.makeText(activity, "Fields backed up", Toast.LENGTH_LONG).show()
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
