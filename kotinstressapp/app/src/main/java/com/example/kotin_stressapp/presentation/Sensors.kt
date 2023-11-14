package com.example.kotin_stressapp.presentation

/*
Courtesy of "How to Use Device Sensors the Right Way in Android - Android Studio Tutorial"
by Phillip Lackner (https://www.youtube.com/watch?v=IU-EAtITRRM&t=1049s)
*/

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor

//Heart Rate Sensor!
class HeartRateSensor(
    context: Context
): AndroidSensor(
    context = context,
    sensorFeature = PackageManager.FEATURE_SENSOR_HEART_RATE,
    sensorType = Sensor.TYPE_HEART_RATE
)