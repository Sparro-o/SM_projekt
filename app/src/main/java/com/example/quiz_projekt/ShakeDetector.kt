package com.example.quiz_projekt

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastShakeTime: Long = 0
    private val shakeThreshold = 12f // Czułość potrząsania
    private val shakeInterval = 1000L // Minimalny czas między wykryciami (ms)

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Oblicz siłę przyspieszenia (bez grawitacji)
            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            val currentTime = System.currentTimeMillis()

            // Jeśli wykryto wystarczająco silne potrząsanie
            if (acceleration > shakeThreshold) {
                if (currentTime - lastShakeTime > shakeInterval) {
                    lastShakeTime = currentTime
                    onShake()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nie jest potrzebne dla tej implementacji
    }
}