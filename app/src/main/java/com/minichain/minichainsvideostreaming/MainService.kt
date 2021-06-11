package com.minichain.minichainsvideostreaming

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket


@Suppress("DEPRECATION")
class MainService : Service() {
    private lateinit var broadcastReceiver: MainServiceBroadcastReceiver
    private lateinit var mediaCodec: MediaCodec

    override fun onCreate() {
        super.onCreate()
        Log.l("MainServiceLog: onCreate service")
        init()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.l("MainServiceLog: onBind service")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.l("MainServiceLog: onDestroy $this")
//        mediaCodec.stop()
//        mediaCodec.release()
        unregisterReceiver(broadcastReceiver)
        removeMainServiceNotification()
    }

    private fun init() {
        Log.l("Init MainService!")

//        mediaCodec = MediaCodec.createEncoderByType("video/avc")
//        val mediaFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720)
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000)
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
//        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
//        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//        mediaCodec.start()

        broadcastReceiver = MainServiceBroadcastReceiver()

        createMainServiceNotification()

        registerMainServiceBroadcastReceiver()
    }

    /**
     * BROADCAST RECEIVER
     **/

    inner class MainServiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.l("MainServiceLog: Broadcast received. Context: " + context + ", intent:" + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.FRAME.toString()) {
//                        Log.l("MainServiceLog: Broadcast Received: $broadcast")
//                        val tempArray: ByteArray = "hello".toByteArray()
                        val tempArray: ByteArray = extras?.getByteArray("byteArray")!!
                        val sendBytesTask = SendBytesTask()

                        val tempArrayEncoded = ByteArray(0)
//                        tempArrayEncoded = encode(tempArray)

                        Log.l("tempArray size: "+ tempArray.size + " bytes")
                        if (tempArrayEncoded != null) {
                            Log.l("tempArrayEncoded size: "+ tempArrayEncoded.size + " bytes")
                        }

                        sendBytesTask.execute(tempArrayEncoded)
                    } else if (broadcast == BroadcastMessage.DEBUG.toString()) {
                        Log.l("MainServiceLog: Broadcast $broadcast received.")
                    } else {
                        Log.l("MainServiceLog: Unknown broadcast received. $broadcast")
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun encode(byteArray: ByteArray): ByteArray? {
        val inputBuffers = mediaCodec.inputBuffers
        val outputBuffers = mediaCodec.outputBuffers

        val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
        Log.l("inputBufferIndex: $inputBufferIndex")

        if (inputBufferIndex >= 0) {
            val inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            inputBuffer.put(byteArray)
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, byteArray.size, 0, 0)
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        Log.l("outputBufferIndex: $outputBufferIndex")
        while (outputBufferIndex >= 0) {
            val outputBuffer = outputBuffers[outputBufferIndex]
            val outData = ByteArray(bufferInfo.size)
            outputBuffer.get(outData)
            Log.l("AvcEncoder " + outData.size.toString() + " bytes written")
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
            return outData
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
        return null
    }

    private fun registerMainServiceBroadcastReceiver() {
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

//    private class EncodeBytesTask : AsyncTask<ByteArray?, Int?, Long?>() {
//        override fun doInBackground(vararg params: ByteArray?): Long? {
//
//        }
//    }

    private class SendBytesTask : AsyncTask<ByteArray?, Int?, Long?>() {
        private val HOST = "192.168.1.46"
        private val PORT = 8000
        private var socket: Socket? = null

        override fun doInBackground(vararg byteArray: ByteArray?): Long {
            Log.l("Async Task. Let's try to send some bytes.")

            val outStream: DataOutputStream?

            try {
                if (byteArray[0] != null && byteArray.isNotEmpty()) {
                    if (socket == null) {
                        Log.l("Creating socket...")
                        socket = Socket()
                        Log.l("Connecting socket...")
                        socket!!.connect(InetSocketAddress(HOST, PORT), 5000)
                    } else if (!socket!!.isConnected) {
                        Log.l("Connecting socket...")
                        socket!!.connect(InetSocketAddress(HOST, PORT), 5000)
                    }

                    if (socket != null && socket!!.isConnected) {
                        Log.l("socket: $socket")
                        Log.l("Creating outStream...")
                        outStream = DataOutputStream(socket!!.getOutputStream())
                        Log.l("outStream: $outStream")
                        outStream.writeInt(byteArray[0]!!.size)
                        outStream.write(byteArray[0])
                        outStream.flush()
//                        Log.l("Send bytes: " + String(byteArray[0]!!) + " to $host:$port")
                        socket!!.close()
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            return 0
        }
    }

    /**
     * SERVICE NOTIFICATION
     **/

    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationName: CharSequence
    private var notificationManager: NotificationManager? = null
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private val serviceNotificationStringId = "MAIN_SERVICE_NOTIFICATION"
    private val serviceNotificationId = 1

    private fun createMainServiceNotification() {
        //Service notification
        notificationName = resources.getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(serviceNotificationStringId, notificationName, importance).apply {
                description = "descriptionText"
            }
            channel.setShowBadge(false)
            //Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            this.notificationManager = notificationManager
        }

        /** Open Main Activity **/
//        //Notification intent to open the activity when pressing the notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, intent.flags)

        notification = NotificationCompat.Builder(this, serviceNotificationStringId)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle("ServiceNotification")
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)

        notificationManagerCompat?.notify(serviceNotificationId, notification.build())
        this.startForeground(serviceNotificationId, notification.build())
    }

    private fun removeMainServiceNotification() {
        if (notificationManagerCompat != null) {
            notificationManagerCompat!!.cancel(serviceNotificationId)
        }
    }
}