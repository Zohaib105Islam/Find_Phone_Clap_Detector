package com.base.find_phone_clap_detector.service.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.base.find_phone_clap_detector.service.DetectorLog
import com.base.find_phone_clap_detector.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

class MotionDetector(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onTriggered: suspend (reason: String) -> Unit
) : SensorEventListener {

    private companion object {
        private const val TAG = "MotionDetector"
        private const val SENSOR_ACTIVATION_DELAY_MS = 5000L
    }

    private var sensorThread: HandlerThread? = null
    private var sensorManager: SensorManager? = null
    private var sensorHandler: Handler? = null
    private var sensorRunnable: Runnable? = null
    private var sensorEventJob: Job? = null
    private var activeSensor: Sensor? = null
    private var isTouchDetectionReady = false
    private var isInPocket = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstRead = true

    fun start(serviceType: String, modeLabel: String) {
        DetectorLog.d(TAG, "start: Initializing sensor detection with delay")
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        activeSensor = when (serviceType) {
            Constants.DONT_TOUCH -> sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            Constants.POCKET_MODE -> sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            else -> sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        isTouchDetectionReady = false
        isFirstRead = true
        sensorHandler?.removeCallbacks(sensorRunnable ?: Runnable { })

        sensorThread?.quitSafely()
        sensorThread = HandlerThread("MotionSensorThread").apply { start() }

        sensorHandler = Handler(Looper.getMainLooper())
        sensorRunnable = Runnable {
            activeSensor?.also {
                val deliveryHandler = sensorThread?.looper?.let { looper -> Handler(looper) }
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, deliveryHandler)
            }
            isTouchDetectionReady = true
            DetectorLog.d(TAG, "start: $modeLabel detection is now active")
        }

        sensorHandler?.postDelayed(sensorRunnable!!, SENSOR_ACTIVATION_DELAY_MS)
    }

    fun stop() {
        sensorHandler?.removeCallbacks(sensorRunnable ?: Runnable { })
        sensorRunnable = null
        sensorHandler = null
        sensorEventJob?.cancel()
        sensorEventJob = null
        sensorManager?.unregisterListener(this)
        isTouchDetectionReady = false
        sensorThread?.quitSafely()
        sensorThread = null
        DetectorLog.d(TAG, "stop: sensor listener removed")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTouchDetectionReady) return
        if (sensorEventJob?.isActive == true) return

        val currentEvent = event ?: return
        val values = currentEvent.values.clone()
        sensorEventJob = coroutineScope.launch(Dispatchers.IO) {
            when (Constants.SERVICE_TYPE) {
                Constants.DONT_TOUCH -> {
                    if (values.size < 3) return@launch
                    val x = currentEvent.values[0]
                    val y = currentEvent.values[1]
                    val z = currentEvent.values[2]

                    if (isFirstRead) {
                        lastX = x
                        lastY = y
                        lastZ = z
                        isFirstRead = false
                    }

                    val deltaX = abs(lastX - x)
                    val deltaY = abs(lastY - y)
                    val deltaZ = abs(lastZ - z)

                    if (deltaX > 1.5f || deltaY > 1.5f || deltaZ > 1.5f) {
                        DetectorLog.d(TAG, "onSensorChanged: motion threshold crossed, triggering alert")
                        onTriggered("motion_threshold")
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }

                Constants.POCKET_MODE -> {
                    if (values.isEmpty()) return@launch
                    val distance = currentEvent.values[0]
                    val maxRange = activeSensor?.maximumRange ?: 0f

                    if (distance < maxRange) {
                        isInPocket = true
                    } else if (isInPocket) {
                        isInPocket = false
                        DetectorLog.d(TAG, "onSensorChanged: pocket mode exit detected, triggering alert")
                        onTriggered("pocket_exit")
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
