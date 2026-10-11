package com.maizemaster.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.text.InputType
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object MaizeGrowthTracker {

    private val stages = arrayOf(
        "Not recorded",
        "Planted",
        "Emergence",
        "Vegetative growth",
        "Tasseling",
        "Silking",
        "Grain filling",
        "Physiological maturity"
    )

    private fun date(value: String): LocalDate? {
        return try {
            LocalDate.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    fun summary(
        field: SavedField,
        today: LocalDate = LocalDate.now()
    ): String {
        val planted = date(field.plantingDate)
            ?: return "🌱 Growth tracking: set your planting date"

        val age = ChronoUnit.DAYS.between(planted, today)
        val stage = field.growthStage.ifBlank {
            "Not recorded"
        }

        val observation =
            if (field.growthObservedDate.isBlank()) {
                ""
            } else {
                " (observed ${field.growthObservedDate})"
            }

        val timing = if (age < 0) {
            "Planting planned in ${-age} days"
        } else {
            "$age days since planting"
        }

        val target = if (field.maturityDays in 1..365) {
            val targetDate = planted.plusDays(
                field.maturityDays.toLong()
            )

            val remaining = ChronoUnit.DAYS.between(
                today,
                targetDate
            )

            val progress = (
                age.coerceAtLeast(0) * 100 /
                    field.maturityDays
                ).coerceIn(0, 100)

            "\nMaturity target: $targetDate\n" +
                if (remaining > 0) {
                    "$remaining days to target" +
                        " • Calendar progress: $progress%"
                } else {
                    "Target date reached" +
                        " • Calendar progress: $progress%"
                }
        } else {
            "\nMaturity target: not set"
        }

        return "🌱 Planted: $planted\n" +
            "$timing\n" +
            "Observed stage: $stage$observation$target"
    }

    fun show(
        activity: Activity,
        storage: FieldStorage,
        fieldId: String,
        onSaved: () -> Unit
    ) {
        val field = storage.loadFields()
            .firstOrNull { it.id == fieldId }
            ?: return

        fun dp(value: Int): Int {
            return (
                value * activity.resources
                    .displayMetrics.density
                ).toInt()
        }

        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }

        fun label(value: String) {
            form.addView(
                TextView(activity).apply {
                    text = value
                    textSize = 15f
                    setPadding(0, dp(8), 0, dp(6))
                }
            )
        }

        var selectedDate = date(field.plantingDate)

        label(summary(field))
        label("Planting date")

        val planting = Button(activity).apply {
            isAllCaps = false
            text = selectedDate?.toString()
                ?: "Choose planting date"

            setOnClickListener {
                val initial = selectedDate
                    ?: LocalDate.now()

                DatePickerDialog(
                    activity,
                    { _, year, month, day ->
                        selectedDate = LocalDate.of(
                            year,
                            month + 1,
                            day
                        )
                        text = selectedDate.toString()
                    },
                    initial.year,
                    initial.monthValue - 1,
                    initial.dayOfMonth
                ).show()
            }
        }

        form.addView(planting)

        label(
            "Days from planting to maturity target " +
                "(optional, 1–365)"
        )

        val days = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Use your variety's maturity target"

            if (field.maturityDays > 0) {
                setText(field.maturityDays.toString())
            }
        }

        form.addView(days)
        label("Stage observed in your field")

        val stage = Spinner(activity).apply {
            adapter = ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                stages
            )

            setSelection(
                stages.indexOf(field.growthStage)
                    .coerceAtLeast(0)
            )
        }

        form.addView(stage)

        val preview = MaizeGrowthView(
            activity,
            field.growthStage
        )

        form.addView(
            preview,
            LinearLayout.LayoutParams(-1, dp(200))
        )

        stage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    preview.setStage(
                        if (position == 0) {
                            ""
                        } else {
                            stages[position]
                        }
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        label(
            "Illustration of the selected stage; " +
                "plant appearance varies in the field."
        )

        label("Field notes")

        val notes = EditText(activity).apply {
            hint = "Plant condition, leaf count or pests"
            inputType =
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(field.growthNotes)
        }

        form.addView(notes)

        label(
            "Calendar progress is a planning estimate. " +
                "Confirm growth stage in the field; " +
                "maturity does not confirm grain is " +
                "ready to harvest."
        )

        val scroll = ScrollView(activity).apply {
            addView(form)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Maize growth • ${field.name}")
            .setView(scroll)
            .setPositiveButton("Save observation", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {
                val planted = selectedDate
                val rawDays = days.text.toString().trim()

                val maturity = if (rawDays.isEmpty()) {
                    0
                } else {
                    rawDays.toIntOrNull()
                }

                val observedStage =
                    if (stage.selectedItemPosition == 0) {
                        ""
                    } else {
                        stage.selectedItem.toString()
                    }

                val today = LocalDate.now()

                when {
                    planted == null -> {
                        Toast.makeText(
                            activity,
                            "Choose a planting date.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    maturity == null ||
                        (
                            rawDays.isNotEmpty() &&
                                maturity !in 1..365
                            ) -> {
                        days.error =
                            "Enter 1–365 days or leave blank"
                    }

                    planted.isAfter(today) &&
                        observedStage.isNotEmpty() -> {
                        Toast.makeText(
                            activity,
                            "Future planting cannot have " +
                                "an observed growth stage.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {
                        val current = storage.loadFields()
                            .firstOrNull {
                                it.id == fieldId
                            }

                        if (current == null) {
                            Toast.makeText(
                                activity,
                                "This field no longer exists.",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()
                        } else {
                            current.plantingDate =
                                planted.toString()
                            current.maturityDays = maturity
                            current.growthStage =
                                observedStage
                            current.growthNotes =
                                notes.text.toString().trim()
                            current.growthObservedDate =
                                if (observedStage.isEmpty()) {
                                    ""
                                } else {
                                    today.toString()
                                }

                            if (storage.updateField(current)) {
                                dialog.dismiss()
                                onSaved()
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Could not save growth tracking.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
    }
}
