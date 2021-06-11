@file:Suppress("DEPRECATION")

package com.minichain.minichainsvideostreaming

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
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
import java.lang.StringBuilder
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var broadcastReceiver: MainActivityBroadcastReceiver
    private lateinit var imageByteArray: ByteArray
    private lateinit var debugTextView: TextView

    private lateinit var usbDeviceConnection: UsbDeviceConnection
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null

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

            val previewFormat = camera.parameters.previewFormat
            val previewSize = camera.parameters.previewSize
            camera.setPreviewCallback { data, camera ->

            }
        }

        val floatingActionButton: View = findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            Toast.makeText(this, "Floating Action Button Pressed!", Toast.LENGTH_SHORT).show()
            sendBroadcast(BroadcastMessage.DEBUG)
        }

        establishUsbConnection()

        Thread {
            while (true) {
                Thread.sleep(1000)
                this@MainActivity.runOnUiThread {
                    updateDebugText()
                }
            }
        }.start()
    }

    private fun establishUsbConnection() {
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        debugTextView = findViewById(R.id.debug_text)

        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        if (usbManager.deviceList.size > 0) {
            usbDevice = deviceList.values.iterator().next()
            if (usbDevice != null) {

                setupConnection()
//                usbDeviceConnection = usbManager.openDevice(usbDevice)
            }
        }
    }

    private fun setupConnection() {
        for (i in 0 until usbDevice!!.interfaceCount step 1) {
            val interfaceClass = usbDevice!!.getInterface(i).interfaceClass
            Log.l("Interface class: " + interfaceClass)
            if (interfaceClass == UsbConstants.USB_CLASS_CDC_DATA) {

            }
        }
    }

    private fun updateDebugText() {
//        Log.l("Update debug text")

        val stringBuilder = StringBuilder()
        if (usbDevice != null) {
            stringBuilder.append(usbManager.deviceList.size.toString() + " USB device connected.")
            stringBuilder.append("\n")
            stringBuilder.append("\nVendor Id: ").append(usbDevice!!.vendorId)
            stringBuilder.append("\nProduct Id: ").append(usbDevice!!.productId)
            stringBuilder.append("\nDevice Class: ").append(usbDevice!!.deviceClass)
            stringBuilder.append("\nDevice Subclass: ").append(usbDevice!!.deviceSubclass)
            stringBuilder.append("\nDevice Protocol: ").append(usbDevice!!.deviceProtocol)
            stringBuilder.append("\n")
        } else {
            stringBuilder.append("No USB device connected.")
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)) {
            stringBuilder.append("\nSystem supports external camera!")
        }

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        stringBuilder.append("\nThere are " + cameraManager.cameraIdList.size + " cameras available.")

        debugTextView.text = stringBuilder.toString()
    }

    private fun sendCurrentFrameToService() {
        val bundle = Bundle()
        bundle.putByteArray("byteArray", imageByteArray)
//        sendBroadcast(BroadcastMessage.FRAME, bundle)
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
        super.onDestroy()
        Log.l("MainActivity: onDestroy $this")
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
//        Log.l("Sending Broadcast $broadcastMessage")
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
            Log.l("MainActivity: Broadcast received. Context: " + context + ", intent:" + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.FRAME.toString()) {
//                        Log.l("MainActivity: Broadcast Received: $broadcast")
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
        Log.l("MainActivity: Register Broadcast Receiver")
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