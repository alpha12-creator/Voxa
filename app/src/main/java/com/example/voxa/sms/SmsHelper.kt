package com.example.voxa.sms

import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SmsHelper {
    private const val TAG = "SmsHelper"

    fun sendSms(context: Context, number: String, text: String, messageId: Long): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val sentIntent = Intent(SmsStatusReceiver.ACTION_SMS_SENT).apply {
                setClass(context, SmsStatusReceiver::class.java)
                putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
            }

            val deliveryIntent = Intent(SmsStatusReceiver.ACTION_SMS_DELIVERED).apply {
                setClass(context, SmsStatusReceiver::class.java)
                putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
            }

            val sentFlags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val deliveryFlags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)

            val parts = smsManager.divideMessage(text)
            if (parts.size > 1) {
                val sentPis = ArrayList<PendingIntent>()
                val deliveryPis = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    val partSentPi = PendingIntent.getBroadcast(
                        context,
                        (messageId * 100 + i).toInt(),
                        sentIntent,
                        sentFlags
                    )
                    val partDeliveryPi = PendingIntent.getBroadcast(
                        context,
                        (messageId * 100 + 50 + i).toInt(),
                        deliveryIntent,
                        deliveryFlags
                    )
                    sentPis.add(partSentPi)
                    deliveryPis.add(partDeliveryPi)
                }
                smsManager.sendMultipartTextMessage(number, null, parts, sentPis, deliveryPis)
            } else {
                val sentPi = PendingIntent.getBroadcast(
                    context,
                    (messageId * 2).toInt(),
                    sentIntent,
                    sentFlags
                )
                val deliveryPi = PendingIntent.getBroadcast(
                    context,
                    (messageId * 2 + 1).toInt(),
                    deliveryIntent,
                    deliveryFlags
                )
                smsManager.sendTextMessage(number, null, text, sentPi, deliveryPi)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $number (messageId: $messageId)", e)
            false
        }
    }

    fun isDefaultSmsApp(context: Context): Boolean {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        return defaultPackage == context.packageName
    }

    fun buildDefaultSmsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                }
            }
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val date = Date(timestamp)

        val sameDayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isToday = sameDayFormat.format(date) == sameDayFormat.format(Date(now))

        return when {
            isToday -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            diff < 2 * 86400000L -> "Yesterday"
            diff < 7 * 86400000L -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }

    fun formatBubbleTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDetailTime(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        val isYesterday = isSameYear && now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1

        return when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            isSameYear -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun isSameDay(time1: Long, time2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
