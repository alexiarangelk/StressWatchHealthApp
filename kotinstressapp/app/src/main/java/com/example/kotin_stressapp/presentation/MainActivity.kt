package com.example.kotin_stressapp.presentation

import android.Manifest
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.kotin_stressapp.presentation.theme.KotinstressappTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.sql.Timestamp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class MainActivity : ComponentActivity() , SensorEventListener {

    private lateinit var sensorManager : SensorManager

    //[ Heart Sensor ]
    private var heartRateSensor : Sensor ?= null

    //Heart Rate
    var heartRateValue by mutableStateOf(0.0f)
    //Heart Rate Variability (HRV)
    var hrvValue by mutableStateOf(0.0)
    var hrvStartTime = System.currentTimeMillis() * 1000000
    var hrvTimeBetween = longArrayOf()
    var hrvArrayCap = 5
//    var hrvStartTime by mutableStateOf(System.currentTimeMillis())

    private val BODY_SENSORS_PERMISSION_CODE = 123

    // ***-----------------[ Main Functions

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                BODY_SENSORS_PERMISSION_CODE
            )
        } else {
            Log.d("Requesting Permission", "Permission already granted")
        }
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
                                    appResuming(appState)
                                }

                            }
                        }
                    }

                    if (appState.value == AppState.RUNNING){
                        Log.d("appState.value == AppState.RUNNING", "Should be displaying the value")

                        DataDisplayText("Heart Rate: $heartRateValue bpm")
                        Spacer(modifier = Modifier.height(2.dp))

                        if(hrvValue == -1.0){
                            DataDisplayText("Heart Rate Variability: Awaiting More Data...")
                        }
                        else{
                            DataDisplayText("Heart Rate Variability: $hrvValue per ms")
                        }
                        DataDisplayText("Heart Rate Variability: $hrvValue per ms")

                        Spacer(modifier = Modifier.height(2.dp))
                        DataDisplayText("Breathing Rate: [] per minute")
                    }
                    else if (appState.value == AppState.PAUSE){
                        DataDisplayText("Heart Rate: $heartRateValue bpm")
                        Spacer(modifier = Modifier.height(2.dp))

                        if(hrvValue == -1.0){
                            DataDisplayText("Heart Rate Variability: Awaiting More Data...")
                        }
                        else{
                            DataDisplayText("Heart Rate Variability: $hrvValue per ms")
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        DataDisplayText("Breathing Rate: [] per minute")
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                }
            }
        }
    }

    // ***-----------------[ State Functions

    fun appRunning(state: MutableState<AppState>) {
        //start button was pressed, what will you do?
        Log.d("State Change", "App is now Running")
        onResume()
        Log.d("Heart Rate Sensor", "grab the heartratevalue $heartRateValue")
    }

    fun appStopping(state: MutableState<AppState>){
        //stop button was pressed, what will you do?
        onPause()
    }

    fun appPausing(state: MutableState<AppState>){
        //pause button was pressed, what will you do?
        onPause()
    }

    fun appResuming(state: MutableState<AppState>){
        //resume button was pressed, what will you do?
        onResume()
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

    @Composable
    fun DataDisplayText(ourDataString: String) {
        Text(
            modifier = Modifier.width(140.dp),
            textAlign = TextAlign.Left,
            color = MaterialTheme.colors.secondary,
            fontSize = 14.sp,
            text = ourDataString
        )
    }

    // ***-----------------[ Sensor Functions


    //TODO: IMPLEMENT PERMISSION!
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BODY_SENSORS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted; proceed with sensor usage
                // ...
            } else {
                // Permission denied; handle accordingly
                // ...
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (heartRateSensor != null){
            Log.d("Heart Rate Sensor", "heartRateSensor is not null, start listener")
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("Heart Rate Sensor", "Stop Listener")
        sensorManager.unregisterListener(this, heartRateSensor)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            Log.d("Heart Rate Sensor", "We're updating values onSensorChanged")
            if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                //heart rate sensor
                heartRateValue = event.values[0]

                //heart rate variability
                val hrvEndTime = event.timestamp
//                hrvValue = TimeUnit.NANOSECONDS.toMillis(hrvEndTime - hrvStartTime);
//                Log.d("HRV Sensor", "hrvStartTime")
//                Log.d("HRV Sensor", hrvStartTime.toString())
//                Log.d("HRV Sensor", "hrvEndTime")
//                Log.d("HRV Sensor", hrvEndTime.toString())
//                Log.d("HRV Sensor", "End Result:")
//                Log.d("HRV Sensor", hrvValue.toString())
                prepareHRV(TimeUnit.NANOSECONDS.toMillis(hrvEndTime - hrvStartTime))
                hrvValue = calculateHRV()
                Log.d("HRV Sensor", "hrvValue to string is...")
                Log.d("HRV Sensor", hrvValue.toString())
                hrvStartTime = hrvEndTime;
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    fun prepareHRV(newInterval : Long){
        //if we don't have _ items in our list of intervals
        if (hrvTimeBetween.size < hrvArrayCap){
            //keep adding to the end
            hrvTimeBetween += newInterval
        }
        else{
            //shift so the most recent number can be added and the furthest one is deleted
            hrvTimeBetween = hrvTimeBetween.copyOfRange(1, hrvTimeBetween.size) + newInterval
        }
//        Log.d("HRV Sensor", hrvTimeBetween.size.toString())
//        Log.d("HRV Sensor", hrvTimeBetween.contentToString())
    }

    fun calculateHRV(): Double{
        //actually calculate HRV
        if (hrvTimeBetween.size < (hrvArrayCap + 3)){
            //we need more time to collect data
            //note, we add the + 3 offset to account for errors in first few data points
            return -1.0
        }
        else{
            return hrvTimeBetween.stdDev()
        }
    }

    fun LongArray.stdDev(): Double {
        val mean = average()
        val sumOfSquaredDifferences = map { (it - mean).toDouble() }.sumOf { it * it }
        return sqrt(sumOfSquaredDifferences / size)
    }
}