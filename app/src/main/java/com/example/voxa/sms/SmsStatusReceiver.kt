package com.example.voxa.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.voxa.data.repository.VoxaRepository

class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId <= 0L) {
            Log.w(TAG, "Received $action with invalid messageId: $messageId")
            return
        }

        val repository = VoxaRepository.getInstance(context)

        when (action) {
            ACTION_SMS_SENT -> {
                val resultCode = resultCode
                Log.d(TAG, "SMS_SENT for msgId=$messageId with resultCode=$resultCode")
                if (resultCode == Activity.RESULT_OK) {
                    repository.onSmsSentSuccess(messageId)
                } else {
                    repository.onSmsSentFailed(messageId, resultCode)
                }
            }
            ACTION_SMS_DELIVERED -> {
                val resultCode = resultCode
                Log.d(TAG, "SMS_DELIVERED for msgId=$messageId with resultCode=$resultCode")
                if (resultCode == Activity.RESULT_OK) {
                    repository.onSmsDeliveredSuccess(messageId)
                } else {
                    repository.onSmsDeliveredFailed(messageId, resultCode)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SmsStatusReceiver"
        const val ACTION_SMS_SENT = "com.example.voxa.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.voxa.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
    }
}
