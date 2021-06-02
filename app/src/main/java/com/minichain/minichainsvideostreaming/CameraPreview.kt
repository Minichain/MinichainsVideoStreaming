package com.minichain.minichainsvideostreaming

import android.content.Context
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

/** A basic Camera preview class */
@Suppress("DEPRECATION")
class CameraPreview(context: Context, private val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback {

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

    var outStream: DataOutputStream? = null

    private fun startPreview() {
        mCamera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()

                /*
                try {
                    outStream = DataOutputStream(Socket("192.168.1.46", 8001).getOutputStream())
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                val previewSize = mCamera.parameters.previewSize
                val previewFormat = mCamera.parameters.previewFormat
                mCamera.setPreviewCallback { data, camera ->
                    // All bytes are in YUV format, therefore, to use the YUV helper functions, we are putting in a YUV object
                    val yuvImage = YuvImage(data, previewFormat, previewSize.width, previewSize.height, null)
                    val rect = Rect(0, 0, previewSize.width, previewSize.height)
                    val outputStream = ByteArrayOutputStream()
                    // Image has now been converted to the jpg format and bytes have been written to the outputStream object
                    yuvImage.compressToJpeg(rect, 80, outputStream)

                    val tempArray: ByteArray = outputStream.toByteArray()

                    if (outStream != null) {
                        Log.l("OutStream: $outStream")
                        outStream!!.writeInt(tempArray.size)
                        outStream!!.write(tempArray)
                        outStream!!.flush()
                    }

//                    Log.l("Frame! data size: " + data.size)
//                    Log.l("Frame rate: " + camera.parameters.previewFrameRate)
                }
                */
                Log.l("Starting Preview")
            } catch (e: IOException) {
                Log.l("Error setting camera preview: ${e.message}")
            }
        }
    }
}