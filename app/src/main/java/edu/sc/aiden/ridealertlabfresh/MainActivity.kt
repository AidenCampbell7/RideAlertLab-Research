package edu.sc.aiden.ridealertlabfresh

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var distanceInput: EditText

    private lateinit var speedInput: EditText

    private lateinit var timeEstimate: TextView
    private lateinit var severitySpinner: Spinner
    private lateinit var vehicleSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var range: TextView
    private var passed = false
    private var lastAlert: AlertResult? = null
    private var speechReady = false
    private var speech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 33, 45)
        window.navigationBarColor = Color.rgb(12, 33, 45)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp, 28.dp, 22.dp, 28.dp)
            setBackgroundColor(Color.rgb(244, 247, 249))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        root.addView(label("RIDE ALERT LAB", 27f, true, Color.rgb(12, 33, 45)))
        root.addView(label("Indoor simulation · no live GPS or pothole detection", 14f, false, Color.DKGRAY))
        root.addView(space(24))

        root.addView(label("Distance to pothole (meters)", 16f, true, Color.rgb(12, 33, 45)))
        distanceInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("200")
            contentDescription = "Distance to pothole in meters"
            setSingleLine(true)
        }
        root.addView(distanceInput)

        root.addView(space(12))
        root.addView(label("Simulated speed (mph)", 16f, true, Color.rgb(12, 33, 45)))
        speedInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("30")
            contentDescription = "Simulated speed in miles per hour"
            setSingleLine(true)
        }
        root.addView(speedInput)


        root.addView(space(12))

        root.addView(label("Pothole severity", 16f, true, Color.rgb(12, 33, 45)))
        severitySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                Severity.entries.map { it.label })
            setSelection(2)
        }
        root.addView(severitySpinner)
        root.addView(space(12))

        root.addView(label("Vehicle", 16f, true, Color.rgb(12, 33, 45)))
        vehicleSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                Vehicle.entries.map { it.label })
            setSelection(1)
        }
        root.addView(vehicleSpinner)
        root.addView(space(20))

        root.addView(button("CHECK WARNING") { passed = false; refresh() })
        root.addView(button("APPROACH 25 M") {
            val current = readDistance() ?: return@button
            if (passed) return@button
            val next = (current - 25).coerceAtLeast(0)
            passed = current == 0 || (next == 0 && current <= 25)
            distanceInput.setText(next.toString())
            refresh()
        })
        root.addView(button("RESET RIDE") {
            passed = false
            distanceInput.setText("200")
            refresh()
        })
        root.addView(space(24))

        status = label("", 24f, true, Color.rgb(12, 33, 45))
        detail = label("", 17f, false, Color.rgb(35, 49, 58))
        range = label("", 14f, false, Color.DKGRAY)
        timeEstimate = label("", 17f, true, Color.rgb(12, 33, 45))
        root.addView(status)
        root.addView(space(10))
        root.addView(detail)
        root.addView(space(10))
        root.addView(range)
        root.addView(space(10))
        root.addView(timeEstimate)
        root.addView(space(20))
        root.addView(button("SPEAK CURRENT ALERT") {
            if (!speechReady) {
                Toast.makeText(this, "Speech is still starting or unavailable", Toast.LENGTH_SHORT).show()
            } else {
                lastAlert?.let {
                    speech?.speak(it.spokenMessage, TextToSpeech.QUEUE_FLUSH, null, "ride-alert")
                }
            }
        })
        root.addView(space(18))
        root.addView(label("Use only while parked. Distances are simulated for this research demo.",
            13f, false, Color.DKGRAY))

        speech = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val support = speech?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
                speechReady = support >= TextToSpeech.LANG_AVAILABLE
            }
        }
        refresh()
    }

    private fun refresh() {
        val distance = readDistance() ?: return
        val speedMph = speedInput.text.toString().toIntOrNull()
        if (speedMph == null || speedMph !in 0..100) {
            speedInput.error = "Enter a whole number from 0 to 100 mph"
            return
        }
        val severity = Severity.entries[severitySpinner.selectedItemPosition]
        val vehicle = Vehicle.entries[vehicleSpinner.selectedItemPosition]
        val result = AlertLogic.evaluate(distance, severity, vehicle, passed)
        lastAlert = result
        status.text = result.title
        status.setTextColor(when (result.level) {
            AlertLevel.CLEAR -> Color.rgb(20, 107, 83)
            AlertLevel.CAUTION -> Color.rgb(169, 102, 0)
            AlertLevel.IMMEDIATE -> Color.rgb(185, 39, 43)
            AlertLevel.PASSED -> Color.rgb(32, 83, 149)
        })
        detail.text = result.explanation
        range.text = "Alert range for this combination: ${result.thresholdMeters} m"
        timeEstimate.text = when {
            passed -> "Estimated time: hazard passed"
            speedMph == 0 -> "Estimated time: stopped"
            else -> {
                val seconds = distance / (speedMph * 0.44704)
                "Estimated time to pothole: %.1f seconds".format(Locale.US, seconds)
            }
        }
    }

    private fun readDistance(): Int? {
        val distance = distanceInput.text.toString().toIntOrNull()
        if (distance == null || distance !in 0..10000) {
            distanceInput.error = "Enter a whole number from 0 to 10,000"
            return null
        }
        return distance
    }

    private fun label(text: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height.dp)
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        speech?.stop()
        speech?.shutdown()
        super.onDestroy()
    }
}
