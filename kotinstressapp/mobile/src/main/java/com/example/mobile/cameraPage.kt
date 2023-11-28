package com.example.mobile

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ImageCaptureException
import com.example.mobile.databinding.ActivityCameraPageBinding
import java.text.SimpleDateFormat
import java.util.Locale
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.math.RoundingMode
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

typealias LumaListener = (luma: Double) -> Unit

class CameraPage : AppCompatActivity(), MessageClient.OnMessageReceivedListener {
    private lateinit var viewBinding: ActivityCameraPageBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService
    var tflite: Interpreter? = null

    private var heart_rate = 0.0
    private var heart_rate_var = 0.0
    private var stressLevel = 0.0

    private lateinit var resultTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private fun loadModelFile(): MappedByteBuffer? {
        val fileDescriptor: AssetFileDescriptor = this.getAssets().openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declareLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            startAutoCapture()
            startListener()
            startDataListener()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        try {
            tflite = Interpreter(loadModelFile()!!)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    private fun getStressLevel(): Any {
        return stressLevel
    }

    private suspend fun processMessage(data: ByteArray) {
        try {
            // Convert the byte array to a String (assuming the data is in string format)
            val message = String(data, Charsets.UTF_8)

            // Depending on your use case, you might want to parse the message as JSON
            val json = JSONObject(message)

            // Extract relevant information from the JSON
            val heartRate = json.optDouble("heart_rate", 0.0)
            val hrv = json.optDouble("heart_rate_variability", 0.0)
        } catch (e: JSONException) {
            Log.e("processMessage", "Error processing message: ${e.message}", e)
        }
    }

    private fun predict(inputData: PyObject?): Float {
        try {
            val array = inputData?.toJava(FloatArray::class.java)
            val allZeros = array?.all { it == 0.0f } ?: false
            if (allZeros) {
                return 0.0f
            }
            val columns = 48
            val rows = 48
            val channels = 1
            val batchSize = 1

            val inputArray = Array(batchSize) { batch ->
                Array(rows) { row ->
                    Array(columns) { col ->
                        FloatArray(channels) { channel ->
                            array?.get((batch * rows * columns + row * columns + col) * channels + channel) ?: 0.0f
                        }
                    }
                }
            }

            val outputTensorIndex = 0
            val outputTensor = tflite?.getOutputTensor(outputTensorIndex)
            val outputShape = outputTensor?.shape()

            if (outputShape?.contentEquals(intArrayOf(1, 1)) == true) {
                // Allocate a 2D array to store the output (1x1)
                val outputArray = Array(batchSize) {
                    FloatArray(1)
                }

                // Run the TensorFlow Lite interpreter
                tflite?.run(inputArray, outputArray)

                // Access the result from the outputArray
                val result = outputArray[0][0]
                Log.d(TAG, "Model Prediction: $result")
                return result
            } else {
                Log.e(TAG, "Error: Invalid output tensor shape - $outputShape")
                return 0.0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting with TensorFlow Lite: ${e.message}", e)
            return 0.0f
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    output.savedUri?.let { processImageWithPython(applicationContext, it) }
                    val resultTextView: TextView = viewBinding.resultTextView
                    val result = getStressLevel()
                    resultTextView.text = "Stress Level: $result"
                    resultTextView.height = 205
                    resultTextView.elevation = 10f
                    resultTextView.invalidate()
                    Log.d("updateUI", "Result: $result")
                }
            }
        )
    }
    private fun startListener() {
        Wearable.getMessageClient(this).addListener { messageEvent ->
            try {
                Log.d(
                    TAG,
                    "Message received. Path: ${messageEvent.path}, Data: ${String(messageEvent.data)}"
                )

                // Use a coroutine to process the message in the background
                CoroutineScope(Dispatchers.IO).launch {
                    processMessage(messageEvent.data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}", e)
            }
        }
    }
    private fun startDataListener() {
        Wearable.getDataClient(this).addListener { dataEvents ->
            for (event in dataEvents) {
                // Handle data event
                if (event.type == DataEvent.TYPE_CHANGED) {
                    // Data item changed
                    val item = event.dataItem
                    // Extract data from the DataMap
                    val dataMapItem = DataMapItem.fromDataItem(item)
                    heart_rate = dataMapItem.dataMap.getString("heart_rate")?.toDoubleOrNull()!!
                    heart_rate_var = dataMapItem.dataMap.getString("heart_rate_var")?.toDoubleOrNull()!!
                    if (heart_rate != null) {
                        Log.d(TAG, (heart_rate as Any).toString())
                    }
                    if (heart_rate_var != null) {
                        Log.d(TAG, (heart_rate_var as Any).toString())
                    }
                }
            }
        }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }
    private val photoCaptureHandler = Handler(Looper.getMainLooper())
    private val photoCaptureRunnable = object : Runnable {
        override fun run() {
            // Take a photo
            takePhoto()
            startListener()
            // Schedule the next photo capture after a delay
            photoCaptureHandler.postDelayed(this, PHOTO_CAPTURE_INTERVAL)
        }
    }

    private val PHOTO_CAPTURE_INTERVAL = 5000L // 5 seconds (adjust as needed)

// ...

    private fun startAutoCapture() {
        // Schedule the first photo capture immediately
        photoCaptureHandler.post(photoCaptureRunnable)
    }

    private fun stopAutoCapture() {
        // Remove any pending callbacks to stop the automatic photo capture
        photoCaptureHandler.removeCallbacks(photoCaptureRunnable)
    }
    private fun processImageWithPython(context: Context, imageUri: Uri) {
        try {
            // Convert content URI to a file path
            val imagePath = convertContentUriToFile(context, imageUri)

            // Import the Python module
            val imageProcessor = Python.getInstance().getModule("faces")

            // Call the Python function to process the image
            val inputData = imageProcessor.callAttr("preprocess", imagePath)

            val result = predict(inputData)
            // Process the stress prediction as needed
            Log.d(TAG, "Stress Prediction: $result")

            stressLevel = calculateProportion(heart_rate, heart_rate_var, result)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image with Python: ${e.message}", e)
        }
    }
    private fun convertContentUriToFile(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (it.moveToFirst()) {
                return it.getString(columnIndex)
            }
        }
        return null
    }
    override fun onMessageReceived(messageEvent : MessageEvent) {
        Log.d(TAG, "message received!!!")
        if (messageEvent.path == "/heart_rate_path") {
            val heartRateData = String(messageEvent.data)
            Log.d(TAG, "Received Heart Rate: $heartRateData")
            val hrvData = String(messageEvent.data)
            Log.d(TAG, "Received Heart Rate Variability: $hrvData")
        }
    }
    //returns percentage
    private fun calculateProportion(hr: Double, hrv: Double, ml: Float): Double{
        var y1 = 0.0
        var y2 = 0.0

        //hr - calculate proportion
        if (hr > 50){
            y1 = (hr - 50)/135
        }

        //hrv - calculate proportion
        if (hrv < 48){
            y2 = hrv/48
        }

        //ml - convert binary to double
        val y3 = if (ml > .5) 1.0 else 0.0

        //final equation
        var final = ((y1 * (.4)) + (y2 * (.4)) + (y3 * (.2))) * 100

        Log.d("yuh", "-----------------------------------------")
        Log.d("Stress Equation", "HR proportion is $y1")
        Log.d("Stress Equation", "HRV proportion is $y2")
        Log.d("Stress Equation", "Machine Learning proportion is $y3")

        return final.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}