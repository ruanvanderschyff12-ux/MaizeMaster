package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class FarmerAccountActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val green = Color.rgb(25, 110, 55)
    private fun dp(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun background(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(14).toFloat()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = green
        val root = ScrollView(this).apply { setBackgroundColor(Color.rgb(246, 250, 246)); isFillViewport = true }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(30), dp(24), dp(30))
        }
        root.addView(form)
        setContentView(root)
        fun title(s: String, size: Float = 17f) = TextView(this).apply {
            text = s
            textSize = size
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(14))
        }
        fun field(hintText: String, type: Int) = EditText(this).apply {
            hint = hintText
            inputType = type
            setTextColor(Color.BLACK)
            setSingleLine(true)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = background(Color.WHITE)
        }
        fun action(label: String, onTap: () -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = background(green)
            setOnClickListener { onTap() }
        }
        form.addView(title("🌽 MaizeMaster Farmer Account", 24f))
        val status = title("", 15f)
        form.addView(status)
        val email = field("Email address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = field("Password (minimum 6 characters)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        form.addView(email)
        form.addView(password)
        val signIn = action("Sign in") {
            val address = email.text.toString().trim()
            val pass = password.text.toString()
            if (address.isEmpty() || pass.isEmpty()) {
                status.text = "Enter your email and password."
            } else {
                status.text = "Signing in..."
                auth.signInWithEmailAndPassword(address, pass).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) finish()
                    else status.text = task.exception?.localizedMessage ?: "Sign in failed."
                }
            }
        }
        val register = action("Create farmer account") {
            val address = email.text.toString().trim()
            val pass = password.text.toString()
            if (address.isEmpty() || pass.length < 6) {
                status.text = "Enter an email and password of at least 6 characters."
            } else {
                status.text = "Creating account..."
                auth.createUserWithEmailAndPassword(address, pass).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) finish()
                    else status.text = task.exception?.localizedMessage ?: "Registration failed."
                }
            }
        }
        form.addView(signIn)
        form.addView(register)
        form.addView(action("Reset password") {
            val address = email.text.toString().trim()
            if (address.isEmpty()) status.text = "Enter your email address first."
            else auth.sendPasswordResetEmail(address).addOnCompleteListener(this) { task ->
                status.text = if (task.isSuccessful) "If the account exists, a reset email will be sent."
                else task.exception?.localizedMessage ?: "Unable to request reset."
            }
        })
        form.addView(action("Sign out") {
            auth.signOut()
            status.text = "Signed out."
            updateStatus(status)
        })
        form.addView(action("Back to MaizeMaster") { finish() })
        updateStatus(status)
    }
    private fun updateStatus(view: TextView) {
        view.text = auth.currentUser?.email?.let { "Signed in as $it" }
            ?: "Sign in or register to prepare for secure cloud saving. Existing fields remain stored on this phone."
    }
}
