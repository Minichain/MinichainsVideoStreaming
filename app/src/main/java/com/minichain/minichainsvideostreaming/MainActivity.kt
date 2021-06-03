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
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var broadcastReceiver: MainActivityBroadcastReceiver
    private lateinit var imageByteArray: ByteArray

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
            val cameraPreview = CameraPreview(this, camera)
            val preview: FrameLayout = this.findViewById(R.id.camera_preview)
            preview.addView(cameraPreview)

            val previewFormat = cameraPreview.mCamera.parameters.previewFormat
            val previewSize = cameraPreview.mCamera.parameters.previewSize
            cameraPreview.mCamera.setPreviewCallback { data, camera ->
                // All bytes are in YUV format, therefore, to use the YUV helper functions, we are putting in a YUV object
                Log.l("Preview format: $previewFormat")
                val yuvImage = YuvImage(data, previewFormat, previewSize.width, previewSize.height, null)
                val rect = Rect(0, 0, previewSize.width, previewSize.height)
                val outputStream = ByteArrayOutputStream()
                // Image has now been converted to the jpg format and bytes have been written to the outputStream object
                yuvImage.compressToJpeg(rect, 80, outputStream)
                imageByteArray = outputStream.toByteArray()
            }
        }

        val floatingActionButton: View = findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            Toast.makeText(this, "Floating Action Button Pressed!", Toast.LENGTH_SHORT).show()
            val bundle = Bundle()
            bundle.putByteArray("byteArray", imageByteArray)
            sendBroadcast(BroadcastMessage.FRAME, bundle)
        }
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
        val id = item.itemId
        return when (id) {
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
            sendBroadcast(broadCastIntent)
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
                    if (broadcast == BroadcastMessage.FRAME.toString()) {
                        Log.l("MainActivity: Broadcast Received: $broadcast")
                    } else {
                        Log.l("MainActivity: Unknown broadcast received")
                    }
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun registerMainActivityBroadcastReceiver() {
        broadcastReceiver = MainActivityBroadcastReceiver()
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }
            registerReceiver(broadcastReceiver, intentFilter)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }
}