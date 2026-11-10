package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MaizeGradingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MaizeCameraActivity::class.java).putExtra("gradingMode", true))
        finish()
    }
}
