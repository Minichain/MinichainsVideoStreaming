package com.minichain.minichainsvideostreaming

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.DataOutputStream
import java.net.Socket


class MainService : Service() {
    private lateinit var broadcastReceiver: MainServiceBroadcastReceiver

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
        unregisterReceiver(broadcastReceiver)
        removeMainServiceNotification()
    }

    private fun init() {
        Log.l("Init MainService!")
        broadcastReceiver = MainServiceBroadcastReceiver()
        registerMainServiceBroadcastReceiver()

        createMainServiceNotification()
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
                        Log.l("MainServiceLog: Broadcast Received: $broadcast")

                        val tempArray: ByteArray = byteArrayOf(0x2E, 0x38)

                        val sendBytesTask = SendBytesTask()
                        sendBytesTask.execute(tempArray)

                    } else {
                        Log.l("MainServiceLog: Unknown broadcast received. $broadcast")
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun registerMainServiceBroadcastReceiver() {
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }

//            intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON)
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG)

            registerReceiver(broadcastReceiver, intentFilter)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * ASYNCTASK
     **/
    private class SendBytesTask : AsyncTask<ByteArray?, Int?, Long?>() {
        override fun doInBackground(vararg byteArray: ByteArray?): Long {
            Log.l("Async Task. Sending Bytes...")

            val outStream: DataOutputStream?

            try {
                if (byteArray[0] != null && byteArray.isNotEmpty()) {
                    outStream = DataOutputStream(Socket("192.168.1.46", 8001).getOutputStream())
                    outStream.writeInt(byteArray[0]!!.size)
                    outStream.write(byteArray[0])
                    outStream.flush()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            return 0
        }

        override fun onProgressUpdate(vararg progress: Int?) {

        }

        override fun onPostExecute(result: Long?) {

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