package com.example.kotin_stressapp.presentation
/*
Courtesy of "How to Use Device Sensors the Right Way in Android - Android Studio Tutorial"
by Phillip Lackner (https://www.youtube.com/watch?v=IU-EAtITRRM&t=1049s)
*/

//to get the sensor values directly
abstract class MeasurableSensor(
    protected val sensorType: Int
) {
    protected var onSensorValuesChanged: ((List<Float>) -> Unit)? = null

    abstract val doesSensorExist: Boolean

    abstract fun startListening()
    abstract fun stopListening()

    fun setOnSensorValuesChangedListener(listener : (List<Float>) -> Unit) {
        onSensorValuesChanged = listener
    }
}