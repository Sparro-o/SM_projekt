package com.example.quiz_projekt

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

    private var lastShakeTime: Long = 0
    private val shakeThreshold = 12f
    private val shakeInterval = 1000L

    private var lastTiltTime: Long = 0
    private val tiltThreshold = 7f
    private val neutralThreshold = 3f
    private val tiltInterval = 1500L
    private var isTilted = false
    private var neutralPositionX = 0f
    private var neutralPositionY = 0f
    private var lastRotationTime: Long = 0
    private var lastRotationZ = 0f
    private val rotationThreshold = 45f
    private val rotationInterval = 1500L
    private var isRotationCalibrated = false

    private var isCalibrated = false

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
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!isCalibrated) {
            neutralPositionX = x
            neutralPositionY = y
            isCalibrated = true
            return
        }

        if (detectShake(x, y, z, currentTime)) {
            return
        }

        detectAnyTilt(x, y, currentTime)
    }

    private fun handleRotation(event: SensorEvent, currentTime: Long) {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val currentRotationZ = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

        if (!isRotationCalibrated) {
            lastRotationZ = currentRotationZ
            isRotationCalibrated = true
            return
        }

        var rotationDifference = abs(currentRotationZ - lastRotationZ)

        if (rotationDifference > 180) {
            rotationDifference = 360 - rotationDifference
        }

        val isNearNeutral = rotationDifference < 10f

        if (isNearNeutral) {
            if (currentTime - lastRotationTime > rotationInterval) {
                lastRotationZ = currentRotationZ
                isRotationCalibrated = true
            }
        } else if (rotationDifference >= rotationThreshold &&
            currentTime - lastRotationTime > rotationInterval) {
            lastRotationTime = currentTime
            lastRotationZ = currentRotationZ
            isTilted = false
            onRotate()
        }
    }

    private fun detectShake(x: Float, y: Float, z: Float, currentTime: Long): Boolean {
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            if (currentTime - lastShakeTime > shakeInterval) {
                lastShakeTime = currentTime
                isTilted = false
                onShake()
                return true
            }
        }
        return false
    }

    private fun detectAnyTilt(x: Float, y: Float, currentTime: Long) {
        val deltaX = abs(x - neutralPositionX)
        val deltaY = abs(y - neutralPositionY)

        val maxDelta = kotlin.math.max(deltaX, deltaY)

        val isTiltedNow = maxDelta > tiltThreshold
        val isNeutralNow = maxDelta < neutralThreshold

        when {
            isTiltedNow && !isTilted && currentTime - lastTiltTime > tiltInterval -> {
                lastTiltTime = currentTime
                isTilted = true
                onTilt()
            }

            isNeutralNow && isTilted -> {
                isTilted = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun resetCalibration() {
        isCalibrated = false
        isRotationCalibrated = false
        isTilted = false
    }
}