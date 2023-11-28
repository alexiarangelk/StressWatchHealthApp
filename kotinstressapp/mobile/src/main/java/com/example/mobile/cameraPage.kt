package com.example.mobile

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import com.example.mobile.databinding.ActivityCameraPageBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import android.os.Handler
import android.os.Looper
import com.chaquo.python.Python
import java.io.File
import java.math.RoundingMode

typealias LumaListener = (luma: Double) -> Unit

class CameraPage : AppCompatActivity() {
    private lateinit var viewBinding: ActivityCameraPageBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dexOutputDir: File = codeCacheDir
        dexOutputDir.setReadOnly()

        viewBinding = ActivityCameraPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            startAutoCapture()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
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
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        val final = calculateProportion(1.0, 0.5, true)
        Log.d("Stress Equation", "grab the HRV $final")
        Log.d("yuh", "-----------------------------------------")

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
            imageProcessor.callAttr("detect_stress", imagePath)
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

    //returns percentage
    private fun calculateProportion(hr : Double, hrv : Double, ml : Boolean): Double{
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
        val y3 = if (ml) 1.0 else 0.0

        //final equation
        var final = ((y1 * (.4)) + (y2 * (.4)) + (y3 * (.2))) * 100

        Log.d("yuh", "-----------------------------------------")
        Log.d("Stress Equation", "HR proportion is $y1")
        Log.d("Stress Equation", "HRV proportion is $y2")
        Log.d("Stress Equation", "Machine Learning proportion is $y3")

        return final.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
    }

}