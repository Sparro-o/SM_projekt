package com.example.quiz_projekt // Użyj swojej nazwy pakietu

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class GestureDetector(
    private val onShake: () -> Unit,
    private val onTiltLeft: () -> Unit,
    private val onTiltRight: () -> Unit
) : SensorEventListener {

    // Parametry dla potrząsania
    private var lastShakeTime: Long = 0
    private val shakeThreshold = 12f
    private val shakeInterval = 1000L

    // Parametry dla przechylenia
    private var lastTiltTime: Long = 0
    private val tiltThreshold = 7f // Czułość przechylenia
    private val tiltInterval = 1500L // 1.5 sekundy między przechyleniami
    private var isTilted = false

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] // Przechylenie lewo/prawo
            val y = event.values[1] // Przechylenie przód/tył
            val z = event.values[2] // Góra/dół

            val currentTime = System.currentTimeMillis()

            // 1. WYKRYWANIE POTRZĄSANIA
            detectShake(x, y, z, currentTime)

            // 2. WYKRYWANIE PRZECHYLENIA
            detectTilt(x, currentTime)
        }
    }

    private fun detectShake(x: Float, y: Float, z: Float, currentTime: Long) {
        // Oblicz całkowitą siłę przyspieszenia
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            if (currentTime - lastShakeTime > shakeInterval) {
                lastShakeTime = currentTime
                isTilted = false // Reset stanu przechylenia
                onShake()
            }
        }
    }

    private fun detectTilt(x: Float, currentTime: Long) {
        // x > 0 = przechylenie w prawo
        // x < 0 = przechylenie w lewo

        if (currentTime - lastTiltTime > tiltInterval) {
            if (x > tiltThreshold && !isTilted) {
                // Przechylenie w PRAWO
                lastTiltTime = currentTime
                isTilted = true
                onTiltRight()
            } else if (x < -tiltThreshold && !isTilted) {
                // Przechylenie w LEWO
                lastTiltTime = currentTime
                isTilted = true
                onTiltLeft()
            } else if (kotlin.math.abs(x) < 2f) {
                // Telefon wrócił do pozycji neutralnej
                isTilted = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nie potrzebne
    }
}