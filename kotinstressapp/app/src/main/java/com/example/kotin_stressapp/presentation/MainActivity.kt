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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.sql.Timestamp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.wear.compose.material.ButtonDefaults
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

class MainActivity : ComponentActivity() , SensorEventListener {

    private lateinit var sensorManager : SensorManager

    //[ Heart Sensor ]
    private var heartRateSensor : Sensor ?= null

    //Heart Rate
    var heartRateValue by mutableStateOf(0.0f)
    //Heart Rate Variability (HRV)
    var hrvValue by mutableStateOf(0.0)
    var hrvRRInterval = floatArrayOf()
    var hrvArrayCap = 8
    var hrvOffset = 0

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
                            onPause()
                            Button(
                                onClick = { //running the app
                                    appState.value = AppState.RUNNING
                                    appRunning(appState)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    MaterialTheme.colors.secondaryVariant
                                )
                            ) {
                                Text("Start")
                            }
                        } else { //while app is running OR paused
                            Button(
                                onClick = { //stopping the app
                                    appState.value = AppState.STOPPED
                                    appStopping(appState)

                                },
                                colors = ButtonDefaults.buttonColors(
                                    MaterialTheme.colors.secondaryVariant
                                )
                            ) {
                                Text("Stop")
                            }

                            //Resume + Pause buttons
                            if (appState.value == AppState.PAUSE) { //app is paused
                                //prepare for resume
                                Button(
                                    onClick = {
                                        appState.value = AppState.RUNNING
                                        appResuming(appState)
                                    },
                                    colors = ButtonDefaults.buttonColors(MaterialTheme.colors.secondaryVariant),
                                    modifier = Modifier.width(70.dp)
                                ) { Text("Resume") }
                            } else { //app is running
                                //prepare for pause
                                Button(
                                    onClick = {
                                        appState.value = AppState.PAUSE
                                        appPausing(appState)
                                    },
                                    colors = ButtonDefaults.buttonColors(MaterialTheme.colors.secondaryVariant),
                                ) { Text("Pause") }
                            }
                        }
                    }

                    if (appState.value == AppState.RUNNING){

                        DataDisplayText("Heart Rate: ", "$heartRateValue bpm", MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.height(2.dp))

                        if(hrvValue == -1.0){
                            DataDisplayText("Heart Rate Variability: ", "Awaiting More Data...", MaterialTheme.colors.error)
                        }
                        else{
                            DataDisplayText("Heart Rate Variability: ", "$hrvValue ms", MaterialTheme.colors.primary)
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        DataDisplayText("Breathing Rate: ", "[] per minute", MaterialTheme.colors.secondary)
                    }
                    else if (appState.value == AppState.PAUSE){
                        DataDisplayText("Heart Rate: ", "$heartRateValue bpm", MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(2.dp))

                        if(hrvValue == -1.0){
                            DataDisplayText("Heart Rate Variability: ", "Awaiting More Data...", MaterialTheme.colors.error)
                        }
                        else{
                            DataDisplayText("Heart Rate Variability: ", "$hrvValue ms", MaterialTheme.colors.error)
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        DataDisplayText("Breathing Rate: ", "[] per minute", MaterialTheme.colors.error)
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                }
            }
        }
    }

    // ***-----------------[ State Functions

    fun appRunning(state: MutableState<AppState>) {
        //start button was pressed, what will you do?
        Log.d("[]", "------------.------------<<=[{}]=>>------------.------------")
        Log.d("State Change", "App is now Running")
        onResume()
    }

    fun appStopping(state: MutableState<AppState>){
        //stop button was pressed, what will you do?
        Log.d("[]", "------------.------------<<=[{}]=>>------------.------------")
        Log.d("State Change", "App is now Stopping")
        onPause()
        //------ send all the data we need onwards to the phone app

        //------ reset data for next recording
        //heart rate
        heartRateValue = 0.0f
        //Heart Rate Variability (HRV)
        hrvValue = 0.0
        hrvRRInterval = hrvRRInterval.drop(hrvRRInterval.size).toTypedArray().toFloatArray()
    }

    fun appPausing(state: MutableState<AppState>){
        //pause button was pressed, what will you do?
        Log.d("[]", "------------.------------<<=[{}]=>>------------.------------")
        Log.d("State Change", "App is now Pausing")
        onPause()
    }

    fun appResuming(state: MutableState<AppState>){
        //resume button was pressed, what will you do?
        Log.d("[]", "------------.------------<<=[{}]=>>------------.------------")
        Log.d("State Change", "App is now Running")
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
            fontWeight = FontWeight.Bold,
            text = greetingName
        )
        Text(
            modifier = Modifier.width(140.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primaryVariant,
            fontSize = 14.sp,
            text = accompanyingName
        )
    }

    @Composable
    fun DataDisplayText(header : String, data : String, dataColor : Color) {
        Text(
            buildAnnotatedString {
                withStyle( style = SpanStyle(
                    color = MaterialTheme.colors.secondary,
                    fontWeight = FontWeight.Bold,

                )){
                    append(header)
                }
                withStyle( style = SpanStyle(
                    color = dataColor,
                )){
                    append(data)
                }
            },
            modifier = Modifier.width(140.dp),
            fontSize = 14.sp,
            textAlign = TextAlign.Left
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
            if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                //heart rate sensor
                heartRateValue = event.values[0]
                Log.d("Heart Rate Sensor", "grab the heartratevalue $heartRateValue")

                //heart rate variability
                val spareHRV = hrvValue
                try {
                    prepareHRV(heartRateValue)
                    hrvValue = calculateHRV()
                    hrvValue = hrvValue.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
                } catch (e : Exception) {
                    Log.d("Heart Rate Variability", "Exception happened, returning $spareHRV")
                    hrvValue = spareHRV
                    // exception happened, just don't return this new bad value
                }

                Log.d("Heart Rate Variability", "grab the HRV $hrvValue")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    //HRV Shenanigans

    fun prepareHRV(newInterval : Float){
        //if we don't have _ items in our list of intervals
        if (hrvRRInterval.size < hrvArrayCap){
            //keep adding to the end
            hrvRRInterval += (60000 / newInterval)
        }
        else{
            //shift so the most recent number can be added and the furthest one is deleted
            hrvRRInterval = hrvRRInterval.copyOfRange(1, hrvRRInterval.size) + (60000 / newInterval)
            //for bad value filtering
            if (hrvOffset < 3){ hrvOffset += 1 }
        }
    }

    fun calculateHRV(): Double{
        //actually calculate HRV
        if ((hrvRRInterval.size + hrvOffset) < (hrvArrayCap + 3)){
            //we need more time to collect data
            //note, we add the + 3 offset to account for errors in first few data points
            return -1.0
        }
        else{
            return calculateRMSSD(hrvRRInterval)
        }
    }

    fun calculateRMSSD(rrIntervals: FloatArray): Double {
        val squaredDifferences = mutableListOf<Double>()

        // Calculate squared differences of successive R-R intervals
        for (i in 1 until rrIntervals.size) {
            val difference = rrIntervals[i] - rrIntervals[i - 1]
            squaredDifferences.add(difference.pow(2).toDouble())
        }

        // Calculate the mean of squared differences
        val meanSquaredDifference = squaredDifferences.average()

        // Calculate the square root of the mean squared difference
        val rmssd = sqrt(meanSquaredDifference)

        return rmssd

    }
}