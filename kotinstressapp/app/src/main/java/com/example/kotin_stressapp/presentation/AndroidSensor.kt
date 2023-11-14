package com.example.kotin_stressapp.presentation
/*
Courtesy of "How to Use Device Sensors the Right Way in Android - Android Studio Tutorial"
by Phillip Lackner (https://www.youtube.com/watch?v=IU-EAtITRRM&t=1049s)
*/

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

abstract class AndroidSensor(
    private val context: Context,
    private val sensorFeature: String,
    sensorType: Int
): MeasurableSensor(sensorType), SensorEventListener {

    override val doesSensorExist: Boolean
    get() = context.packageManager.hasSystemFeature(sensorFeature);

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun startListening(){
        if (!doesSensorExist){
            //make sure the sensor we passed in exists!
            return
        }
        if(!::sensorManager.isInitialized && sensor == null){
            //is this the first time we are calling the sensors
            sensorManager = context.getSystemService(SensorManager::class.java) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }
        //actually start listening
        sensor?.let{
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun stopListening() {
        if(!doesSensorExist || !::sensorManager.isInitialized){
            return
        }
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!doesSensorExist){
            return
        }
        if(event?.sensor?.type == sensorType){
            onSensorValuesChanged?.invoke(event.values.toList())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

}