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

    /**
     * New-phone onboarding: download only when there are no local fields.
     * Never overwrite another account's or an existing phone's fields silently.
     */
    fun syncOnSignIn(activity: Activity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = activity.getSharedPreferences("maizemaster_fields", Context.MODE_PRIVATE)
        val owner = prefs.getString(OWNER_KEY, null)
        if (owner == uid) return
        val local = FieldStorage(activity).loadFields()
        if (local.isNotEmpty()) {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Fields already on this phone")
                .setMessage("This phone contains ${local.size} saved fields. They have not been replaced or uploaded. Use Cloud Restore to download your account's fields, or back up the phone's fields only if they belong to your account.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        FirebaseFirestore.getInstance().collection("farmers").document(uid)
            .collection("backups").document("fields").get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@addOnSuccessListener
                if (FieldStorage(activity).loadFields().isNotEmpty()) return@addOnSuccessListener
                val raw = snapshot.get("fields") as? List<*>
                if (raw == null) {
                    Toast.makeText(activity, "No cloud fields yet. Your new fields can be backed up from this phone.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val restored = parseCloudFields(raw)
                if (restored == null) {
                    Toast.makeText(activity, "Cloud fields could not be read", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                if (FieldStorage(activity).saveFields(restored)) {
                    prefs.edit().putString(OWNER_KEY, uid).apply()
                    Toast.makeText(activity, "${restored.size} fields downloaded from your account. Automatic upload enabled.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Cloud download unavailable. Local fields are safe; try Cloud Restore later.", Toast.LENGTH_LONG).show()
            }
    }

    private fun parseCloudFields(raw: List<*>): List<SavedField>? = try {
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
    } catch (_: Exception) { null }

    // Pull remote changes on return to My Fields, but require consent before replacing local data.
    private var refreshInProgress = false
    private var lastPromptedVersion: String? = null

    fun refreshFromCloud(activity: Activity, onUpdated: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = activity.getSharedPreferences("maizemaster_fields", Context.MODE_PRIVATE)
        if (prefs.getString(OWNER_KEY, null) != uid || refreshInProgress) return
        refreshInProgress = true
        FirebaseFirestore.getInstance().collection("farmers").document(uid)
            .collection("backups").document("fields")
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnCompleteListener { task ->
                refreshInProgress = false
                if (!task.isSuccessful || activity.isFinishing ||
                    FirebaseAuth.getInstance().currentUser?.uid != uid) return@addOnCompleteListener
                val raw = task.result?.get("fields") as? List<*> ?: return@addOnCompleteListener
                val remote = parseCloudFields(raw) ?: return@addOnCompleteListener
                val local = FieldStorage(activity).loadFields()
                val remoteMaps = remote.map { fieldToMap(it) }
                if (remoteMaps == local.map { fieldToMap(it) }) {
                    lastPromptedVersion = null
                    return@addOnCompleteListener
                }
                val version = uid + remoteMaps.toString()
                if (lastPromptedVersion == version) return@addOnCompleteListener
                lastPromptedVersion = version
                android.app.AlertDialog.Builder(activity)
                    .setTitle("Cloud field changes available")
                    .setMessage("Your account has ${remote.size} cloud fields and this phone has ${local.size} local fields. Downloading will replace this phone's field list. Choose Keep local if you have unsaved-to-cloud changes.")
                    .setNegativeButton("Keep local", null)
                    .setPositiveButton("Download cloud fields") { _, _ ->
                        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@setPositiveButton
                        // Recheck local data so we do not overwrite edits made while dialog was open.
                        if (FieldStorage(activity).loadFields().map { fieldToMap(it) } != local.map { fieldToMap(it) }) {
                            Toast.makeText(activity, "Local fields changed. Please refresh again.", Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (FieldStorage(activity).saveFields(remote)) {
                            onUpdated()
                            Toast.makeText(activity, "Cloud fields downloaded", Toast.LENGTH_LONG).show()
                        }
                    }.show()
            }
    }

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
                val fields = parseCloudFields(raw)
                if (fields == null) {
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
