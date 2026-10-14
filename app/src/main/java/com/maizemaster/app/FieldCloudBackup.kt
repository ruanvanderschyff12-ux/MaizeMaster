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
}
