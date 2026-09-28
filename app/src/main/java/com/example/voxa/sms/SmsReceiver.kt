package com.example.voxa.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.voxa.MainActivity
import com.example.voxa.R
import com.example.voxa.data.preferences.VoxaPreferences
import com.example.voxa.data.repository.VoxaRepository

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val repository = VoxaRepository.getInstance(context)
        val preferences = VoxaPreferences(context)
        val settings = preferences.settings.value

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
            val body = sms.displayMessageBody ?: sms.messageBody ?: ""
            val timestamp = sms.timestampMillis

            repository.receiveIncomingSms(sender, body, timestamp)

            if (settings.notificationsEnabled) {
                showNotification(context, sender, body, settings.hideMessagePreview)
            }
        }
    }

    private fun showNotification(context: Context, sender: String, body: String, hidePreview: Boolean) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channelId = "voxa_messages_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VOXA Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming message notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val displayText = if (hidePreview) "New message received" else body

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(sender)
            .setContentText(displayText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(sender.hashCode(), notification)
    }
}
