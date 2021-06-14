@file:Suppress("DEPRECATION")

package com.minichain.minichainsvideostreaming

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var broadcastReceiver: MainActivityBroadcastReceiver
    private lateinit var imageByteArray: ByteArray
    private var streaming = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Permissions.checkAndRequestPermissions(this)) {
            init()
        }
    }

    override fun onStart() {
        Log.l("MainActivity: onStart")
        super.onStart()
        registerMainActivityBroadcastReceiver()
    }

    override fun onPause() {
        Log.l("MainActivity: onPause")
        super.onPause()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("MainActivity: error un-registering receiver $e")
        }
    }

    private fun init() {
        Log.l("Init MainActivity!")

        setContentView(R.layout.activity_main)

        //Start service:
        val serviceIntent = Intent(applicationContext, MainService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                applicationContext.startForegroundService(serviceIntent)
                Log.l("Start Foreground Service")
            } catch (e: Exception) {
                applicationContext.startService(serviceIntent)
                Log.l("Start Service")
            }
        } else {
            Log.l("Start Service")
            applicationContext.startService(serviceIntent)
        }

        registerMainActivityBroadcastReceiver()

        val itHasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (itHasCamera) {
            Log.l("" + Camera.getNumberOfCameras() + " cameras detected!")
        } else {
            Log.e("No camera detected!")
        }

        val camera = getCameraInstance()

        Log.l("Camera: $camera")
        if (camera != null) {
            Log.l("Camera Preview Size: " + camera.parameters.previewSize.toString())

            camera.setDisplayOrientation(90)
            camera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            camera.parameters.previewFrameRate = 24
            val cameraPreview = CameraPreview(this, camera)
            val preview: FrameLayout = this.findViewById(R.id.camera_preview)
            preview.addView(cameraPreview)

            val previewFormat = cameraPreview.mCamera.parameters.previewFormat
            val previewSize = cameraPreview.mCamera.parameters.previewSize
            cameraPreview.mCamera.setPreviewCallback { data, camera ->
                Thread {
                    // All bytes are in YUV format, therefore, to use the YUV helper functions, we are putting in a YUV object
                    Log.l("Preview format: $previewFormat")
                    val yuvImage = YuvImage(data, previewFormat, previewSize.width, previewSize.height, null)
                    val rect = Rect(0, 0, previewSize.width, previewSize.height)
                    val outputStream = ByteArrayOutputStream()
                    // Image has now been converted to the jpg format and bytes have been written to the outputStream object
                    yuvImage.compressToJpeg(rect, 5, outputStream)
                    imageByteArray = outputStream.toByteArray()
                    sendCurrentFrameToService()
                }.start()
            }
        }

        val floatingActionButton: View = findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            Toast.makeText(this, "Floating Action Button Pressed!", Toast.LENGTH_SHORT).show()
            sendBroadcast(BroadcastMessage.START_STOP_STREAMING)
        }
    }

    private fun sendCurrentFrameToService() {
        val bundle = Bundle()
        bundle.putByteArray("byteArray", imageByteArray)
        sendBroadcast(BroadcastMessage.FRAME, bundle)
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dropdown_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_option -> {
                true
            }
            R.id.exit_app_option -> {
                closeApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun closeApp() {
        onDestroy()
        Process.killProcess(Process.myPid())
        exitProcess(-1)
    }

    override fun onDestroy() {
        Log.l("onDestroy $this")
        super.onDestroy()
        // If there is a Service running...
        val serviceIntent = Intent(applicationContext, MainService::class.java)
        applicationContext.stopService(serviceIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.l("Request Permissions Result, requestCode: $requestCode, permissions: $permissions, grantResults: $grantResults")
        when (requestCode) {
            1 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                } else {
                    closeApp()
                }
            }
        }
    }

    private fun sendBroadcast(broadcastMessage: BroadcastMessage) {
        sendBroadcast(broadcastMessage, null)
    }

    private fun sendBroadcast(broadcastMessage: BroadcastMessage, bundle: Bundle?) {
        Log.l("Sending Broadcast $broadcastMessage")
        try {
            val broadCastIntent = Intent()
            broadCastIntent.action = broadcastMessage.toString()
            if (bundle != null) {
                broadCastIntent.putExtras(bundle)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadCastIntent)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    inner class MainActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.l("MainActivity:: Broadcast received. Context: " + context + ", intent:" + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    when (broadcast) {
                        BroadcastMessage.FRAME.toString() -> {
                            Log.l("MainActivity: Broadcast Received: $broadcast")
                        }
                        BroadcastMessage.UPDATE_VIEWS.toString() -> {
                            Log.l("MainActivity: Broadcast Received: $broadcast")
                            updateViews(extras)
                        }
                        else -> {
                            Log.l("MainActivity: Unknown broadcast received")
                        }
                    }
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateViews(extras: Bundle?) {
        streaming = extras!!.getBoolean("streaming")
        val stringBuilder = StringBuilder()
        if (streaming) {
            stringBuilder.append("Streaming in progress.")
        } else {
            stringBuilder.append("Streaming paused.")
        }
        val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList
        if (cameraIdList.isNotEmpty()) {
            stringBuilder.append("\n" + cameraIdList.size + " cameras detected.")
            for (i in cameraIdList.indices step 1) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraIdList[i])
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                stringBuilder.append("\nCamera " + i + ": ")
                when (lensFacing) {
                    0 -> stringBuilder.append("Facing back.")
                    1 -> stringBuilder.append("Facing front.")
                    else -> stringBuilder.append("External camera.")
                }
            }
        } else {
            stringBuilder.append("\nNo cameras detected.")
        }
        findViewById<TextView>(R.id.debug_text).text = stringBuilder.toString()
    }

    private fun registerMainActivityBroadcastReceiver() {
        broadcastReceiver = MainActivityBroadcastReceiver()
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }
}