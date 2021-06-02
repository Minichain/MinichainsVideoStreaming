@file:Suppress("DEPRECATION")

package com.minichain.minichainsvideostreaming

import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Permissions.checkAndRequestPermissions(this)) {
            init()
        }
    }

    private fun init() {
        Log.l("Init MainActivity!")

        setContentView(R.layout.activity_main)

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
        }

        val floatingActionButton: View = findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            Toast.makeText(this, "Floating Action Button Pressed!", Toast.LENGTH_SHORT).show()
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
}