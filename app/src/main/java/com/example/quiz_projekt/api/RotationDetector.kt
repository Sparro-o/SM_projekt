package com.example.quiz_projekt.api

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class RotationDetector(private val onRotation: () -> Unit) : SensorEventListener {

    private var lastRotationZ: Float = 0f
    private var isInitialized = false
    private var lastRotationTime: Long = 0
    private val rotationInterval = 1500L // Minimum 1.5 sekundy między wykryciami
    private val rotationThreshold = 60f // Minimum 60 stopni obrotu

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            // Konwertuj wektor rotacji na macierz rotacji
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Pobierz kąty orientacji (azymut, pitch, roll)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // orientationAngles[2] to roll (obrót wokół osi Z) w radianach
            val currentRotationZ = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            if (!isInitialized) {
                lastRotationZ = currentRotationZ
                isInitialized = true
                return
            }

            val currentTime = System.currentTimeMillis()

            // Oblicz różnicę w rotacji
            var rotationDifference = abs(currentRotationZ - lastRotationZ)

            // Obsługa przejścia przez 180/-180 stopni
            if (rotationDifference > 180) {
                rotationDifference = 360 - rotationDifference
            }

            // Wykryj obrót o ~90 stopni (z tolerancją 60-120 stopni)
            if (rotationDifference >= rotationThreshold &&
                currentTime - lastRotationTime > rotationInterval) {

                lastRotationTime = currentTime
                lastRotationZ = currentRotationZ
                onRotation()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nie jest potrzebne dla tej implementacji
    }

    fun reset() {
        isInitialized = false
    }
}