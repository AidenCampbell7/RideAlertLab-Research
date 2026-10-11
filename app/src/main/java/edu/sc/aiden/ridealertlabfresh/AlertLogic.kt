package edu.sc.aiden.ridealertlabfresh

enum class Severity(val label: String, val warningMeters: Int) {
    MINOR("Minor", 55), MODERATE("Moderate", 90), SEVERE("Severe", 130)
}

enum class Vehicle(val label: String, val extraMeters: Int) {
    CAR("Car", 0), MOTORCYCLE("Motorcycle", 40), SCOOTER("Scooter", 25)
}

enum class AlertLevel { CLEAR, CAUTION, IMMEDIATE, PASSED }

data class AlertResult(
    val level: AlertLevel,
    val thresholdMeters: Int,
    val title: String,
    val explanation: String,
    val spokenMessage: String
)

/** Classroom simulation only. This app does not read GPS or detect real potholes. */
object AlertLogic {
    fun evaluate(distanceMeters: Int, severity: Severity, vehicle: Vehicle, passed: Boolean): AlertResult {
        val threshold = severity.warningMeters + vehicle.extraMeters
        val context = "${severity.label.lowercase()} pothole, ${vehicle.label.lowercase()}"
        return when {
            passed -> AlertResult(
                AlertLevel.PASSED, threshold, "Hazard passed",
                "You have passed the simulated pothole. Reset to try another ride.",
                "Hazard passed"
            )
            distanceMeters <= threshold / 2 -> AlertResult(
                AlertLevel.IMMEDIATE, threshold, "Pothole very close",
                "$context · $distanceMeters m ahead. Slow smoothly; keep your eyes on the road.",
                "Pothole very close. Slow down smoothly."
            )
            distanceMeters <= threshold -> AlertResult(
                AlertLevel.CAUTION, threshold, "Pothole ahead",
                "$context · $distanceMeters m ahead. Prepare to slow down.",
                "Pothole ahead in $distanceMeters meters."
            )
            else -> AlertResult(
                AlertLevel.CLEAR, threshold, "No nearby alert",
                "$context · $distanceMeters m ahead. Warning starts at $threshold m.",
                "No nearby alert"
            )
        }
    }
}
