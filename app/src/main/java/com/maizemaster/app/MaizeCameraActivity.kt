package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*

class MaizeCameraActivity : Activity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private val cameraRequest = 4101
    private val galleryRequest = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val green = Color.rgb(18, 96, 51)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(245, 250, 246))
        }
        fun heading(value: String, size: Float): TextView = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(green)
            setPadding(0, 10, 0, 16)
        }
        root.addView(heading("🌽 MaizeMate Crop Scanner", 25f))
        root.addView(heading("Photograph maize leaves, plants, cobs or kernels. Photo diagnosis is not active yet.", 15f))
        preview = ImageView(this).apply {
            setBackgroundColor(Color.rgb(225, 238, 225))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Selected maize photo preview"
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        status = heading("Select a photo to preview it.", 14f)
        root.addView(status)
        root.addView(Button(this).apply {
            text = "📷 Take maize photo"
            setOnClickListener {
                try {
                    startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), cameraRequest)
                } catch (e: Exception) {
                    status.text = "No camera app available."
                }
            }
        })
        root.addView(Button(this).apply {
            text = "🖼 Choose existing photo"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, galleryRequest)
            }
        })
        root.addView(Button(this).apply {
            text = "← Back to dashboard"
            setOnClickListener { finish() }
        })
        setContentView(root)
    }

    @Deprecated("Uses Android activity result callback for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        try {
            if (requestCode == cameraRequest) {
                val bitmap = data.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    preview.setImageBitmap(bitmap)
                    status.text = "Photo captured. AI analysis will be connected in the next stage."
                } else status.text = "Camera returned no preview."
            } else if (requestCode == galleryRequest) {
                val uri = data.data ?: return
                preview.setImageURI(uri)
                status.text = "Photo selected. AI analysis will be connected in the next stage."
            }
        } catch (e: Exception) {
            status.text = "Unable to display photo. Try another image."
        }
    }
}
