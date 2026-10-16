package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class FarmerAccountActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var layout: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }
        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
        status = TextView(this).apply { textSize = 17f }
        layout.addView(TextView(this).apply { text = "🌽 Farmer Account"; textSize = 26f })
        layout.addView(status)
        val syncStatus = TextView(this).apply { textSize = 15f }
        layout.addView(syncStatus)
        fun showSyncStatus() {
            val state = getSharedPreferences("maizemaster_sync_state", MODE_PRIVATE)
            val detail = state.getString("status", null) ?: "Not checked yet"
            val pending = FieldStorage(this).pendingDeletedIds().size
            val label = when {
                auth.currentUser == null -> "Not signed in"
                detail.startsWith("Syncing") -> "🔄 Syncing"
                detail.startsWith("Sync complete") -> "✅ Synced (last attempt)"
                detail.startsWith("Sync unavailable") || detail.contains("failed", true) -> "⚠️ Sync needs attention"
                else -> "☁️ Sync status"
            }
            syncStatus.text = "$label\n$detail" + if (pending > 0) "\n⏳ $pending field deletion(s) waiting to sync" else ""
        }
        showSyncStatus()
        val email = EditText(this).apply {
            hint = "Email address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val password = EditText(this).apply {
            hint = "Password (at least 6 characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(email)
        layout.addView(password)
        fun credentialsValid(): Boolean {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches() ||
                password.text.length < 6) {
                status.text = "Enter a valid email and a password of at least 6 characters."
                return false
            }
            return true
        }
        layout.addView(Button(this).apply {
            text = "Sign in"
            setOnClickListener {
                if (!credentialsValid()) return@setOnClickListener
                isEnabled = false
                auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                    .addOnCompleteListener { task ->
                        isEnabled = true
                        if (task.isSuccessful) {
                            password.text.clear()
                            refresh()
                        } else status.text = "Sign in failed: ${task.exception?.localizedMessage ?: "Try again"}"
                    }
            }
        })
        layout.addView(Button(this).apply {
            text = "Register new farmer"
            setOnClickListener {
                if (!credentialsValid()) return@setOnClickListener
                isEnabled = false
                auth.createUserWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                    .addOnCompleteListener { task ->
                        isEnabled = true
                        if (task.isSuccessful) {
                            password.text.clear()
                            refresh()
                        } else status.text = "Registration failed: ${task.exception?.localizedMessage ?: "Try again"}"
                    }
            }
        })
        layout.addView(Button(this).apply {
            text = "Reset password"
            setOnClickListener {
                val address = email.text.toString().trim()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                    status.text = "Enter your email address first."
                    return@setOnClickListener
                }
                auth.sendPasswordResetEmail(address).addOnCompleteListener {
                    status.text = if (it.isSuccessful) "If the account exists, a reset email has been requested."
                        else "Could not request reset: ${it.exception?.localizedMessage ?: "Try again"}"
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Sync fields now (safe merge)"
            setOnClickListener {
                if (auth.currentUser == null) {
                    status.text = "Sign in first."
                    return@setOnClickListener
                }
                isEnabled = false
                status.text = "Syncing fields..."
                FieldCloudSync(this@FarmerAccountActivity).sync { result ->
                    isEnabled = true
                    status.text = result
                    showSyncStatus()
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Sign out"
            setOnClickListener {
                AlertDialog.Builder(this@FarmerAccountActivity)
                    .setTitle("Sign out?")
                    .setMessage("Your locally saved fields will stay on this phone. Cloud syncing requires signing in.")
                    .setPositiveButton("Sign out") { _, _ -> auth.signOut(); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        })
        // Review retained copies only after sync. No automatic deletion.
        layout.addView(TextView(this).apply {
            text = "🌽 Conflict review"
            textSize = 22f
        })
        val reviewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(reviewContainer)
        fun showConflictReview() {
            reviewContainer.removeAllViews()
            val storage = FieldStorage(this)
            val fields = storage.loadFields()
            val copies = fields.filter { it.name.endsWith(" (conflict copy)") }
            if (copies.isEmpty()) {
                reviewContainer.addView(TextView(this).apply { text = "No conflict copies to review." })
            }
            for (copy in copies) {
                val originalName = copy.name.removeSuffix(" (conflict copy)")
                val candidates = fields.filter { it.id != copy.id && it.name == originalName }
                reviewContainer.addView(TextView(this).apply {
                    text = "Review: $originalName\nConflict version: ${copy.name}\nArea: ${copy.width} × ${copy.length} m; yield: ${copy.expectedYield} t/ha" +
                        if (candidates.size == 1) "\nOriginal: ${candidates[0].name}; area: ${candidates[0].width} × ${candidates[0].length} m; yield: ${candidates[0].expectedYield} t/ha" else "\nOriginal not uniquely identified — both fields will be kept."
                })

                reviewContainer.addView(Button(this).apply {
                    text = "Keep conflict version as separate field"
                    setOnClickListener {
                        AlertDialog.Builder(this@FarmerAccountActivity)
                            .setTitle("Keep both fields?")
                            .setMessage("Rename the conflict copy to a normal separate field. The original remains unchanged.")
                            .setPositiveButton("Keep both") { _, _ ->
                                storage.updateField(copy.copy(name = "$originalName (saved alternative)"))
                                showConflictReview()
                                FieldCloudSync(this@FarmerAccountActivity).sync { showSyncStatus() }
                            }
                            .setNegativeButton("Cancel", null).show()
                    }
                })
                reviewContainer.addView(Button(this).apply {
                    text = "Discard conflict copy"
                    setOnClickListener {
                        AlertDialog.Builder(this@FarmerAccountActivity)
                            .setTitle("Discard this version?")
                            .setMessage("Delete only ${copy.name}? This also syncs the deletion to your other devices.")
                            .setPositiveButton("Delete copy") { _, _ ->
                                storage.deleteField(copy.id)
                                showConflictReview()
                                FieldCloudSync(this@FarmerAccountActivity).sync { showSyncStatus() }
                            }
                            .setNegativeButton("Cancel", null).show()
                    }
                })

            }
        }
        showConflictReview()
        layout.addView(TextView(this).apply {
            text = "Field sync runs when the dashboard opens and after supported field saves. Use manual sync to retry. Offline edits remain saved locally; reopen the dashboard after internet returns. Conflicting edits are preserved as separate copies. Field deletions are queued for cloud sync; use a temporary test field until verified."
        })
        refresh()
    }

    private fun refresh() {
        val user = auth.currentUser
        status.text = if (user == null) "Not signed in" else "Signed in: ${user.email ?: "Farmer"}"
    }
}
