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
        layout.addView(MaizeUi.header(this, "ACCOUNT & SYNC", "Farmer Account",
            "Sign in and sync saved field edits across your phones."))
        layout.addView(status)
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
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Sign out"
            setOnClickListener {
                AlertDialog.Builder(this@FarmerAccountActivity)
                    .setTitle("Sign out?")
                    .setMessage("Your locally saved fields will stay on this phone. Cloud syncing requires pressing Sync fields now.")
                    .setPositiveButton("Sign out") { _, _ -> auth.signOut(); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        })
        layout.addView(TextView(this).apply {
            text = "Use Sync fields now on each phone to send and receive saved field edits. Offline edits stay on your phone until you sync online. If both phones edit the same field, your local edit is kept as a conflict copy. Field deletions are local."
        })
        refresh()
    }

    private fun refresh() {
        val user = auth.currentUser
        status.text = if (user == null) "Not signed in" else "Signed in: ${user.email ?: "Farmer"}"
    }
}
