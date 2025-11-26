package com.example.quiz_projekt // Użyj swojej nazwy pakietu

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import kotlin.math.abs

class GestureDetector(
    private val onShake: () -> Unit,
    private val onTilt: () -> Unit,
    private val onRotate: () -> Unit
) : SensorEventListener {

    // Parametry dla potrząsania
    private var lastShakeTime: Long = 0
    private val shakeThreshold = 12f
    private val shakeInterval = 1000L

    // Parametry dla przechylenia (dowolny kierunek)
    private var lastTiltTime: Long = 0
    private val tiltThreshold = 6f
    private val tiltInterval = 1500L
    private var isTilted = false
    private var neutralPositionX = 0f
    private var neutralPositionY = 0f

    // Parametry dla obrotu (rotation wokół osi Z)
    private var lastRotationTime: Long = 0
    private var lastRotationZ = 0f
    private val rotationThreshold = 45f // Minimum 45 stopni obrotu
    private val rotationInterval = 1500L
    private var isRotationCalibrated = false

    private var isCalibrated = false

    // Flaga do sprawdzenia typu sensora
    private var sensorType: Int = -1

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        sensorType = event.sensor.type
        val currentTime = System.currentTimeMillis()

        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometer(event, currentTime)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                handleRotation(event, currentTime)
            }
        }
    }

    private fun handleAccelerometer(event: SensorEvent, currentTime: Long) {
        val x = event.values[0] // Przechylenie lewo/prawo
        val y = event.values[1] // Przechylenie przód/tył
        val z = event.values[2] // Góra/dół

        // Kalibracja - zapisz pozycję neutralną przy pierwszym odczycie
        if (!isCalibrated) {
            neutralPositionX = x
            neutralPositionY = y
            isCalibrated = true
            return
        }

        // 1. WYKRYWANIE POTRZĄSANIA (priorytet)
        if (detectShake(x, y, z, currentTime)) {
            return // Jeśli wykryto potrząsanie, nie sprawdzaj przechylenia
        }

        // 2. WYKRYWANIE PRZECHYLENIA W DOWOLNYM KIERUNKU
        detectAnyTilt(x, y, currentTime)
    }

    private fun handleRotation(event: SensorEvent, currentTime: Long) {
        // Konwertuj rotation vector na kąty orientacji
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // orientationAngles[0] = azimuth (rotation around Z axis) w radianach
        val currentRotationZ = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

        if (!isRotationCalibrated) {
            lastRotationZ = currentRotationZ
            isRotationCalibrated = true
            return
        }

        if (currentTime - lastRotationTime > rotationInterval) {
            // Oblicz różnicę w rotacji
            var rotationDifference = abs(currentRotationZ - lastRotationZ)

            // Obsługa przejścia przez 180/-180 stopni
            if (rotationDifference > 180) {
                rotationDifference = 360 - rotationDifference
            }

            // Wykryj obrót o minimum 45 stopni
            if (rotationDifference >= rotationThreshold) {
                lastRotationTime = currentTime
                lastRotationZ = currentRotationZ
                isTilted = false // Reset innych gestów
                onRotate()
            }
        }
    }

    private fun detectShake(x: Float, y: Float, z: Float, currentTime: Long): Boolean {
        // Oblicz całkowitą siłę przyspieszenia
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            if (currentTime - lastShakeTime > shakeInterval) {
                lastShakeTime = currentTime
                isTilted = false // Reset stanu przechylenia
                onShake()
                return true
            }
        }
        return false
    }

    private fun detectAnyTilt(x: Float, y: Float, currentTime: Long) {
        if (currentTime - lastTiltTime > tiltInterval) {

            // Oblicz różnicę od pozycji neutralnej
            val deltaX = abs(x - neutralPositionX)
            val deltaY = abs(y - neutralPositionY)

            // Sprawdź czy jest wystarczające przechylenie w DOWOLNYM kierunku
            val isTiltedNow = deltaX > tiltThreshold || deltaY > tiltThreshold

            if (isTiltedNow && !isTilted) {
                // Wykryto nowe przechylenie
                lastTiltTime = currentTime
                isTilted = true
                onTilt()

            } else if (!isTiltedNow && isTilted) {
                // Telefon wrócił do pozycji neutralnej - gotowy na nowe przechylenie
                isTilted = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nie potrzebne
    }

    fun resetCalibration() {
        isCalibrated = false
        isRotationCalibrated = false
        isTilted = false
    }
}