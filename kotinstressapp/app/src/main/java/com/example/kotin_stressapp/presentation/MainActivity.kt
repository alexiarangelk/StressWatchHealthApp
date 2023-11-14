package com.example.kotin_stressapp.presentation

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.kotin_stressapp.presentation.theme.KotinstressappTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager : SensorManager
    private var heartRateSensor : Sensor ?= null
    private var heartRateValue : Float = 0.0f
    private val sensorListener = SensorListener()

    // ***-----------------[ Main Functions

    override fun onCreate(savedInstanceState: Bundle?) {

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        super.onCreate(savedInstanceState)
        setContent {
            WearApp("Android")
        }
    }

    @Composable
    fun WearApp(
        greetingName: String
    ) {
        KotinstressappTheme {
            /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
             * version of LazyColumn for wear devices with some added features. For more information,
             * see d.android.com/wear/compose.
             */
            var appState = remember { mutableStateOf(AppState.STOPPED) }
            //var appState: AppState by rememberMutableStateOf(AppState.STOPPED)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(MaterialTheme.colors.background),

                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {

                    Spacer(modifier = Modifier.height(25.dp))

                    if (appState.value == AppState.STOPPED){
                        Greeting(greetingName = "Welcome to StressNav!", accompanyingName = "Press Start to Begin Recording Data")
                    }
                    else if (appState.value == AppState.RUNNING){
                        Greeting(greetingName = "StressNav", accompanyingName = "Currently Running")
                    }
                    else if (appState.value == AppState.PAUSE){
                        Greeting(greetingName = "StressNav", accompanyingName = "App is now Paused")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row content
                        if (appState.value == AppState.STOPPED) { //while app is not running
                            Button(
                                onClick = { //running the app
                                    Log.d("Button - Start", "button clicked")
                                    appState.value = AppState.RUNNING
                                    appRunning(appState)
                                }
                            ) {
                                Text("Start")
                            }
                        } else { //while app is running OR paused
                            Log.d("Button - Stop/Pause", "button should have changed")
                            Button(
                                onClick = { //stopping the app
                                    appState.value = AppState.STOPPED
                                    appStopping(appState)

                                }
                            ) {
                                Text("Stop")
                            }

                            //Resume + Pause buttons
                            Button(
                                onClick = {
                                    if (appState.value == AppState.PAUSE) { //app is paused
                                        //unpause it
                                        appState.value = AppState.RUNNING
                                    } else { //app is running
                                        //pause it
                                        appState.value = AppState.PAUSE
                                        appPausing(appState)
                                    }
                                }
                            ) {
                                if (appState.value == AppState.PAUSE) { //app is paused
                                    //prepare for resume
                                    Text("Resume")
                                } else { //app is running
                                    //prepare for pause
                                    Text("Pause")
                                }

                            }
                        }
                    }

                    if (appState.value == AppState.RUNNING){
                        Log.d("appState.value == AppState.RUNNING", "Should be displaying the value")
                        Text(
                            modifier = Modifier.width(140.dp),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colors.secondary,
                            fontSize = 14.sp,
                            text = "Heart Rate:$heartRateValue bpm"
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            modifier = Modifier.width(140.dp),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colors.secondary,
                            fontSize = 14.sp,
                            text = "Heart Rate Variability: [] per minute"
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            modifier = Modifier.width(140.dp),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colors.secondary,
                            fontSize = 14.sp,
                            text = "Breathing Rate: [] per minute"
                        )
                    }
                    else if (appState.value == AppState.PAUSE){
                        Text(
                            modifier = Modifier.width(140.dp),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colors.secondary,
                            fontSize = 14.sp,
                            text = "Heart Rate:$heartRateValue bpm"
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            modifier = Modifier.width(140.dp),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colors.secondary,
                            fontSize = 14.sp,
                            text = "Heart Rate Variability: [] per minute"
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            modifier = Modifier.width(140.dp),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colors.secondary,
                            fontSize = 14.sp,
                            text = "Breathing Rate: [] per minute"
                        )
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                }
            }
        }
    }

    // ***-----------------[ State Functions

    fun appRunning(state: MutableState<AppState>) {
        //start button was pressed, what will you do?
        while (state.value == AppState.RUNNING) {
            if (heartRateSensor != null){
                onResume()
                heartRateValue = sensorListener.getHeartRateValue()
            }
        }
    }

    fun appStopping(state: MutableState<AppState>){
        //stop button was pressed, what will you do?
        while (state.value == AppState.STOPPED) {
            break
        }
    }

    fun appPausing(state: MutableState<AppState>){
        //pause button was pressed, what will you do?
        while (state.value == AppState.PAUSE) {
            break
        }
    }

    fun appResuming(state: MutableState<AppState>){
        //resume button was pressed, what will you do?

    }

    // ***-----------------[ Sensor Functions


    //TODO: IMPLEMENT PERMISSION!
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        if (requestCode == BODY_SENSORS_PERMISSION_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted; proceed with sensor usage
//                // ...
//            } else {
//                // Permission denied; handle accordingly
//                // ...
//            }
//        }
//    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(sensorListener, heartRateSensor, SensorManager.SENSOR_STATUS_ACCURACY_LOW)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener, heartRateSensor)
    }

    // ***-----------------[ Misc

    @Composable
    fun Greeting(greetingName: String, accompanyingName: String) {
        Text(
            modifier = Modifier.width(100.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = MaterialTheme.colors.primary,
            text = greetingName
        )
        Text(
            modifier = Modifier.width(140.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.secondary,
            fontSize = 14.sp,
            text = accompanyingName
        )
    }
}

// Separate class implementing SensorEventListener
class SensorListener : SensorEventListener {
    private var heartRateValue : Float = 0.0f

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                heartRateValue = event.values[0]
            }
        }
    }

    fun getHeartRateValue(): Float{
        return heartRateValue
    }
}
