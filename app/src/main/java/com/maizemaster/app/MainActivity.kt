package com.maizemaster.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "🌽 MaizeMaster"
            textSize = 28f
        }

        val hectares = EditText(this).apply {
            hint = "Field size in hectares"
            inputType = 8194
        }

        val yield = EditText(this).apply {
            hint = "Expected tons per hectare"
            inputType = 8194
        }

        val result = TextView(this).apply {
            text = "Your estimated harvest will appear here"
            textSize = 20f
        }

        val calculate = Button(this).apply {
            text = "Calculate Harvest"
            setOnClickListener {
                val area = hectares.text.toString()
                    .toDoubleOrNull() ?: 0.0
                val tons = yield.text.toString()
                    .toDoubleOrNull() ?: 0.0

                result.text =
                    "Estimated harvest: %.2f tons"
                        .format(area * tons)
            }
        }

        layout.addView(title)
        layout.addView(hectares)
        layout.addView(yield)
        layout.addView(calculate)
        layout.addView(result)

        setContentView(layout)
    }
}
