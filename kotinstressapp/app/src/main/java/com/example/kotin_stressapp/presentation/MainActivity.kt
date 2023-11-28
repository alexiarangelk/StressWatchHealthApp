package com.example.kotin_stressapp.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.kotin_stressapp.presentation.theme.KotinstressappTheme
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import org.json.JSONException
import org.json.JSONObject
import java.math.RoundingMode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : ComponentActivity() , SensorEventListener {

    private lateinit var sensorManager : SensorManager

    //[ Heart Sensor ]
    private var heartRateSensor : Sensor ?= null

    //Heart Rate
    var heartRateValue by mutableStateOf(0.0f)
    val hrMap = mutableMapOf<String, Float>()
    //Heart Rate Variability (HRV)
    var hrvValue by mutableStateOf(0.0)
    var hrvRRInterval = floatArrayOf()
    var hrvArrayCap = 8
    var hrvOffset = 0
    val hrvMap = mutableMapOf<String, Double>()

    private val BODY_SENSORS_PERMISSION_CODE = 123
    //var connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // ***-----------------[ Main Functions

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        var relativeHumiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

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

        try {
            val `object` = JSONObject()
            `object`.put("heart_rate", hrMap.toString())
            `object`.put("heart_rate_variability", hrvMap.toString())
            MessageSender("/MessageChannel", `object`.toString(), applicationContext).start()

        } catch (e: JSONException) {
            Log.d("Data Sending", "Failed to send data to mobile device")
        }

        //------ reset data for next recording
        //heart rate
        heartRateValue = 0.0f
        //Heart Rate Variability (HRV)
        hrvValue = 0.0
        hrvRRInterval = hrvRRInterval.drop(hrvRRInterval.size).toTypedArray().toFloatArray()

        hrMap.clear()
        hrvMap.clear()


    }

//

    fun sendDataToPhone() {
        // Mock node ID for emulation purposes
        val nodeId = "mockNodeId"

        val path = "/heart_rate_path"

        prepareHRV(heartRateValue)
        var hrvValueToSend = calculateHRV()
        hrvValueToSend = hrvValueToSend.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()

        // Combine heart rate and HRV into a single string to send in array
        val data = "HR: $heartRateValue, HRV: $hrvValueToSend"
        val dataBytes = data.toByteArray()

        // Get an instance of the Wearable MessageClient
        val messageClient = Wearable.getMessageClient(this)

        // Send the message
        messageClient.sendMessage(nodeId, path, dataBytes).apply {
            addOnSuccessListener {
                Log.d("sendDataToPhone", "Message sent successfully")
            }
            addOnFailureListener {
                Log.e("sendDataToPhone", "Failed to send message", it)
            }
        }
    }

    internal class MessageSender(var path: String, var message: String, var context: Context) :
        Thread() {
        override fun run() {
            try {
                val nodeListTask: Task<List<Node>> = Wearable.getNodeClient(
                    context.applicationContext
                ).connectedNodes
                val nodes: List<Node> = Tasks.await<List<Node>>(nodeListTask)
                val payload = message.toByteArray()
                for (node in nodes) {
                    val nodeId: String = node.getId()
                    val sendMessageTask = Wearable.getMessageClient(
                        context
                    ).sendMessage(nodeId, path, payload)
                    try {
                        Tasks.await(sendMessageTask)
                    } catch (exception: java.lang.Exception) {
                        // TODO: Implement exception handling
                        Log.e(TAG, "Exception thrown")
                    }
                }
            } catch (exception: java.lang.Exception) {
                Log.e(TAG, exception.message!!)
            }
        }

        companion object {
            private const val TAG = "MessageSender"
        }
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

                val timeNow = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(
                    TimeZone.getDefault().toZoneId()).format(Instant.now()).toString()

                //heart rate sensor
                heartRateValue = event.values[0]
                hrMap[timeNow] = heartRateValue
                Log.d("Heart Rate Sensor", "grab the heartratevalue $heartRateValue")
                Log.d("Heart Rate Sensor", "hr map is $hrMap")



                //heart rate variability
                val spareHRV = hrvValue
                try {
                    prepareHRV(heartRateValue)
                    hrvValue = calculateHRV()
                    hrvValue = hrvValue.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
                    hrvMap[timeNow] = hrvValue
                } catch (e : Exception) {
                    Log.d("Heart Rate Variability", "Exception happened, returning $spareHRV")
                    hrvValue = spareHRV
                    hrvMap[timeNow] = -1.0  //returning error indicator
                }
                Log.d("Heart Rate Variability", "grab the HRV $hrvValue")
                Log.d("Heart Rate Variability Sensor", "hrv map is $hrvMap")

                sendDataToPhone()
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