package com.base.find_phone_clap_detector.utils

import android.content.Context
import android.hardware.*
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.math.sqrt

class SensorBasedPhoneInHandDetector(
    private val context: Context
) : PhoneInHandDetector {

    override suspend fun isPhoneInHand(): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            if (sensorManager == null) {
                Log.e("📱InHandDetector", "❌ SensorManager not available")
                cont.resume(false)
                return@suspendCancellableCoroutine
            }

            val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            if (accelSensor == null) Log.w("📱InHandDetector", "⚠️ Accelerometer missing")
            if (gravitySensor == null) Log.w(
                "📱InHandDetector",
                "⚠️ Gravity sensor missing (will fallback to accelerometer only)"
            )
            if (proximitySensor == null) Log.w("📱InHandDetector", "⚠️ Proximity sensor missing")

            if (accelSensor == null) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }

            val accelData = mutableListOf<Float>()
            val gravityData = mutableListOf<Float>()
            var isNear = false

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            val mag = sqrt(
                                event.values[0] * event.values[0] +
                                        event.values[1] * event.values[1] +
                                        event.values[2] * event.values[2]
                            )
                            accelData.add(mag)
                        }

                        Sensor.TYPE_GRAVITY -> {
                            val g = sqrt(
                                event.values[0] * event.values[0] +
                                        event.values[1] * event.values[1] +
                                        event.values[2] * event.values[2]
                            )
                            gravityData.add(g)
                        }

                        Sensor.TYPE_PROXIMITY -> {
                            isNear = proximitySensor != null &&
                                    event.values[0] < (proximitySensor.maximumRange * 0.8f)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            // ✅ Register only if sensor exists
            try {
                sensorManager.registerListener(
                    listener,
                    accelSensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
                gravitySensor?.let {
                    sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                }
                proximitySensor?.let {
                    sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
                Log.d("📱InHandDetector", "✅ Sensor listeners registered safely")
            } catch (e: Exception) {
                Log.e("📱InHandDetector", "❌ registerListener failed: ${e.message}")
                cont.resume(false)
                return@suspendCancellableCoroutine
            }

            val job = CoroutineScope(Dispatchers.Default).launch {
                delay(3000)
                sensorManager.unregisterListener(listener)

                if (accelData.isEmpty()) {
                    Log.w("📱InHandDetector", "⚠️ No accelerometer samples received")
                    cont.resume(false)
                    return@launch
                }

                val accelMean = accelData.average().toFloat()
                val accelVar =
                    accelData.map { (it - accelMean) * (it - accelMean) }.average().toFloat()

                val gravityVar = if (gravityData.isNotEmpty()) {
                    val mean = gravityData.average().toFloat()
                    gravityData.map { (it - mean) * (it - mean) }.average().toFloat()
                } else 0f

                Log.d(
                    "📱InHandDetector",
                    "📊 Stats → accelVar=$accelVar, gravityVar=$gravityVar, isNear=$isNear"
                )

                val motionDetected = accelVar > 0.05f && accelVar < 3.0f
                val orientationStable = gravityVar < 0.2f || gravitySensor == null
                val inHand = (isNear || motionDetected) && orientationStable

                Log.i("📱InHandDetector", "✅ InHand detection result → $inHand")
                cont.resume(inHand)
            }

            cont.invokeOnCancellation {
                job.cancel()
                sensorManager.unregisterListener(listener)
                Log.d("📱InHandDetector", "🧹 Cleaned up listener on cancellation")
            }
        }
    }

}
