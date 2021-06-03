package com.minichain.minichainsvideostreaming

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

/** A basic Camera preview class */
@Suppress("DEPRECATION")
class CameraPreview(context: Context, val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback {

    private val mHolder: SurfaceHolder = holder.apply {
        // Install a SurfaceHolder.Callback so we get notified when the
        // Underlying surface is created and destroyed.
        addCallback(this@CameraPreview)
        // Deprecated setting, but required on Android versions prior to 3.0
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Empty. Take care of releasing the Camera preview in your activity.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.surface == null) {
            // Preview surface does not exist
            return
        }

        // Stop preview before making changes
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            // Ignore: tried to stop a non-existent preview
        }

        // Set preview size and make any resize, rotate or
        // Reformatting changes here

        startPreview()
    }

    private fun startPreview() {
        mCamera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
                Log.l("Starting Preview")
            } catch (e: IOException) {
                Log.l("Error setting camera preview: ${e.message}")
            }
        }
    }
}