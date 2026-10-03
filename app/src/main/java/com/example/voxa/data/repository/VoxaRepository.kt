package com.example.voxa.data.repository

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.voxa.data.database.VoxaDbHelper
import com.example.voxa.data.model.Conversation
import com.example.voxa.data.model.Message
import com.example.voxa.data.model.MessageDirection
import com.example.voxa.data.model.MessageStatus
import com.example.voxa.data.model.MessageType
import com.example.voxa.sms.SmsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoxaRepository private constructor(private val context: Context) {
    private val dbHelper = VoxaDbHelper(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()

    private var activeConversationId: Long? = null

    init {
        scope.launch {
            dbHelper.purgeDemoData(getWritableDb())
            syncDeviceSms()
            refreshConversations()
        }
    }

    private fun getReadableDb(): SQLiteDatabase = dbHelper.readableDatabase
    private fun getWritableDb(): SQLiteDatabase = dbHelper.writableDatabase

    fun refreshConversations() {
        scope.launch {
            val list = queryConversations()
            _conversations.value = list
        }
    }

    fun openConversation(convId: Long) {
        activeConversationId = convId
        markAsRead(convId)
        loadMessages(convId)
    }

    fun closeConversation() {
        activeConversationId = null
        _currentMessages.value = emptyList()
    }

    private fun loadMessages(convId: Long) {
        scope.launch {
            val list = queryMessages(convId)
            _currentMessages.value = list
        }
    }

    private suspend fun queryConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val db = getReadableDb()
        val list = mutableListOf<Conversation>()
        val cursor: Cursor = db.query(
            VoxaDbHelper.TABLE_CONVERSATIONS,
            null,
            null,
            null,
            null,
            null,
            "${VoxaDbHelper.COL_CONV_LAST_TIME} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.toConversation())
            }
        }
        list
    }

    private suspend fun queryMessages(convId: Long): List<Message> = withContext(Dispatchers.IO) {
        val db = getReadableDb()
        val list = mutableListOf<Message>()
        val cursor: Cursor = db.query(
            VoxaDbHelper.TABLE_MESSAGES,
            null,
            "${VoxaDbHelper.COL_MSG_CONV_ID} = ?",
            arrayOf(convId.toString()),
            null,
            null,
            "${VoxaDbHelper.COL_MSG_TIME} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.toMessage())
            }
        }
        list
    }

    suspend fun getStarredMessages(): List<Pair<Conversation, Message>> = withContext(Dispatchers.IO) {
        val db = getReadableDb()
        val list = mutableListOf<Pair<Conversation, Message>>()
        val query = """
            SELECT m.*, c.name, c.number, c.is_archived, c.is_deleted
            FROM ${VoxaDbHelper.TABLE_MESSAGES} m
            JOIN ${VoxaDbHelper.TABLE_CONVERSATIONS} c ON m.${VoxaDbHelper.COL_MSG_CONV_ID} = c.${VoxaDbHelper.COL_CONV_ID}
            WHERE m.${VoxaDbHelper.COL_MSG_STARRED} = 1
            ORDER BY m.${VoxaDbHelper.COL_MSG_TIME} DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val msg = it.toMessage()
                val conv = Conversation(
                    id = msg.conversationId,
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    number = it.getString(it.getColumnIndexOrThrow("number")),
                    isArchived = it.getInt(it.getColumnIndexOrThrow("is_archived")) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow("is_deleted")) == 1
                )
                list.add(conv to msg)
            }
        }
        list
    }

    fun sendMessage(
        convId: Long,
        text: String,
        type: MessageType = MessageType.TEXT,
        dataUri: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        contactName: String? = null,
        contactNumber: String? = null
    ) {
        scope.launch {
            val db = getWritableDb()
            val now = System.currentTimeMillis()

            val conv = queryConversationById(convId) ?: return@launch
            if (conv.isBlocked) return@launch

            // State model: SENDING -> SENT -> DELIVERED, or FAILED
            val initialStatus = if (type == MessageType.TEXT) MessageStatus.SENDING else MessageStatus.SENT

            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_CONV_ID, convId)
                put(VoxaDbHelper.COL_MSG_DIR, MessageDirection.OUT.name)
                put(VoxaDbHelper.COL_MSG_TEXT, text)
                put(VoxaDbHelper.COL_MSG_TIME, now)
                put(VoxaDbHelper.COL_MSG_STARRED, 0)
                put(VoxaDbHelper.COL_MSG_STATUS, initialStatus.name)
                put(VoxaDbHelper.COL_MSG_TYPE, type.name)
                put(VoxaDbHelper.COL_MSG_DATA_URI, dataUri)
                put(VoxaDbHelper.COL_MSG_FILE_NAME, fileName)
                put(VoxaDbHelper.COL_MSG_FILE_SIZE, fileSize)
                put(VoxaDbHelper.COL_MSG_CONTACT_NAME, contactName)
                put(VoxaDbHelper.COL_MSG_CONTACT_NUMBER, contactNumber)
            }
            val msgId = db.insert(VoxaDbHelper.TABLE_MESSAGES, null, cv)

            val previewText = when (type) {
                MessageType.IMAGE -> "Photo"
                MessageType.FILE -> "Attachment: ${fileName ?: "File"}"
                MessageType.CONTACT -> "Contact: ${contactName ?: "Contact"}"
                MessageType.VOICE -> "🎤 Voice message"
                MessageType.TEXT -> text
            }

            // Update conversation last text and time
            val convCv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, previewText)
                put(VoxaDbHelper.COL_CONV_LAST_TIME, now)
                put(VoxaDbHelper.COL_CONV_LAST_DIR, "out")
            }
            db.update(
                VoxaDbHelper.TABLE_CONVERSATIONS,
                convCv,
                "${VoxaDbHelper.COL_CONV_ID} = ?",
                arrayOf(convId.toString())
            )

            refreshConversations()
            if (activeConversationId == convId) {
                loadMessages(convId)
            }

            // Attempt actual device SMS send if type == TEXT
            if (type == MessageType.TEXT) {
                val sent = SmsHelper.sendSms(context, conv.number, text, msgId)
                if (!sent) {
                    // System refused or immediate exception -> mark FAILED
                    updateMessageStatus(msgId, MessageStatus.FAILED)
                }
                // When sent succeeds, the system will broadcast ACTION_SMS_SENT once carrier receives it,
                // and ACTION_SMS_DELIVERED once recipient receives it. No fake delays or false ticks.
            }
        }
    }

    fun resendMessage(msgId: Long) {
        scope.launch {
            val msg = queryMessageById(msgId) ?: return@launch
            val conv = queryConversationById(msg.conversationId) ?: return@launch
            if (conv.isBlocked) return@launch

            val db = getWritableDb()
            val now = System.currentTimeMillis()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.SENDING.name)
                put(VoxaDbHelper.COL_MSG_TIME, now)
            }
            db.update(VoxaDbHelper.TABLE_MESSAGES, cv, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(msgId.toString()))

            val convCv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, msg.text)
                put(VoxaDbHelper.COL_CONV_LAST_TIME, now)
                put(VoxaDbHelper.COL_CONV_LAST_DIR, "out")
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, convCv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(conv.id.toString()))

            refreshConversations()
            if (activeConversationId == conv.id) {
                loadMessages(conv.id)
            }

            val sent = SmsHelper.sendSms(context, conv.number, msg.text, msg.id)
            if (!sent) {
                updateMessageStatus(msgId, MessageStatus.FAILED)
            }
        }
    }

    fun onSmsSentSuccess(messageId: Long) {
        scope.launch {
            val msg = queryMessageById(messageId) ?: return@launch
            // Transition from SENDING to SENT
            if (msg.status == MessageStatus.SENDING) {
                val db = getWritableDb()
                val cv = ContentValues().apply {
                    put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.SENT.name)
                }
                db.update(VoxaDbHelper.TABLE_MESSAGES, cv, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(messageId.toString()))
                activeConversationId?.let { loadMessages(it) }
            }
        }
    }

    fun onSmsSentFailed(messageId: Long, resultCode: Int) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.FAILED.name)
            }
            db.update(VoxaDbHelper.TABLE_MESSAGES, cv, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(messageId.toString()))
            activeConversationId?.let { loadMessages(it) }
        }
    }

    fun onSmsDeliveredSuccess(messageId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.DELIVERED.name)
            }
            db.update(VoxaDbHelper.TABLE_MESSAGES, cv, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(messageId.toString()))
            activeConversationId?.let { loadMessages(it) }
        }
    }

    fun onSmsDeliveredFailed(messageId: Long, resultCode: Int) {
        // As per specification: If the message is SENT but no delivery confirmation arrives, keep it as SENT.
        // Do NOT convert to DELIVERED or falsely to FAILED unless there is an actual confirmed send failure.
    }

    fun scheduleMessage(convId: Long, text: String, scheduledTime: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_CONV_ID, convId)
                put(VoxaDbHelper.COL_MSG_DIR, MessageDirection.OUT.name)
                put(VoxaDbHelper.COL_MSG_TEXT, text)
                put(VoxaDbHelper.COL_MSG_TIME, scheduledTime)
                put(VoxaDbHelper.COL_MSG_STARRED, 0)
                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.SCHEDULED.name)
                put(VoxaDbHelper.COL_MSG_SCHEDULED_TIME, scheduledTime)
                put(VoxaDbHelper.COL_MSG_TYPE, MessageType.TEXT.name)
            }
            db.insert(VoxaDbHelper.TABLE_MESSAGES, null, cv)

            val convCv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, "[Scheduled] $text")
                put(VoxaDbHelper.COL_CONV_LAST_TIME, scheduledTime)
                put(VoxaDbHelper.COL_CONV_LAST_DIR, "out")
            }
            db.update(
                VoxaDbHelper.TABLE_CONVERSATIONS,
                convCv,
                "${VoxaDbHelper.COL_CONV_ID} = ?",
                arrayOf(convId.toString())
            )

            refreshConversations()
            if (activeConversationId == convId) {
                loadMessages(convId)
            }
        }
    }

    suspend fun getScheduledMessages(): List<Pair<Conversation, Message>> = withContext(Dispatchers.IO) {
        val db = getReadableDb()
        val list = mutableListOf<Pair<Conversation, Message>>()
        val query = """
            SELECT m.*, c.name, c.number, c.is_archived, c.is_deleted
            FROM ${VoxaDbHelper.TABLE_MESSAGES} m
            JOIN ${VoxaDbHelper.TABLE_CONVERSATIONS} c ON m.${VoxaDbHelper.COL_MSG_CONV_ID} = c.${VoxaDbHelper.COL_CONV_ID}
            WHERE m.${VoxaDbHelper.COL_MSG_STATUS} = '${MessageStatus.SCHEDULED.name}'
            ORDER BY m.${VoxaDbHelper.COL_MSG_SCHEDULED_TIME} ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val msg = it.toMessage()
                val conv = Conversation(
                    id = msg.conversationId,
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    number = it.getString(it.getColumnIndexOrThrow("number")),
                    isArchived = it.getInt(it.getColumnIndexOrThrow("is_archived")) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow("is_deleted")) == 1
                )
                list.add(conv to msg)
            }
        }
        list
    }

    fun sendScheduledMessageNow(msgId: Long) {
        scope.launch {
            val msg = queryMessageById(msgId) ?: return@launch
            val conv = queryConversationById(msg.conversationId) ?: return@launch
            val now = System.currentTimeMillis()
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.SENDING.name)
                put(VoxaDbHelper.COL_MSG_TIME, now)
                putNull(VoxaDbHelper.COL_MSG_SCHEDULED_TIME)
            }
            db.update(VoxaDbHelper.TABLE_MESSAGES, cv, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(msgId.toString()))

            val convCv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, msg.text)
                put(VoxaDbHelper.COL_CONV_LAST_TIME, now)
                put(VoxaDbHelper.COL_CONV_LAST_DIR, "out")
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, convCv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(conv.id.toString()))

            refreshConversations()
            activeConversationId?.let { loadMessages(it) }

            val ok = SmsHelper.sendSms(context, conv.number, msg.text, msg.id)
            if (!ok) {
                updateMessageStatus(msgId, MessageStatus.FAILED)
            }
        }
    }

    fun checkScheduledMessages() {
        scope.launch {
            val db = getWritableDb()
            val now = System.currentTimeMillis()
            val cursor = db.query(
                VoxaDbHelper.TABLE_MESSAGES,
                null,
                "${VoxaDbHelper.COL_MSG_STATUS} = ? AND ${VoxaDbHelper.COL_MSG_SCHEDULED_TIME} <= ?",
                arrayOf(MessageStatus.SCHEDULED.name, now.toString()),
                null,
                null,
                null
            )
            val due = mutableListOf<Message>()
            cursor.use {
                while (it.moveToNext()) {
                    due.add(it.toMessage())
                }
            }

            if (due.isNotEmpty()) {
                due.forEach { msg ->
                    val conv = queryConversationById(msg.conversationId)
                    val cv = ContentValues().apply {
                        put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.SENDING.name)
                        put(VoxaDbHelper.COL_MSG_TIME, now)
                        putNull(VoxaDbHelper.COL_MSG_SCHEDULED_TIME)
                    }
                    db.update(
                        VoxaDbHelper.TABLE_MESSAGES,
                        cv,
                        "${VoxaDbHelper.COL_MSG_ID} = ?",
                        arrayOf(msg.id.toString())
                    )

                    if (conv != null && !conv.isBlocked) {
                        val sent = SmsHelper.sendSms(context, conv.number, msg.text, msg.id)
                        if (!sent) {
                            val failCv = ContentValues().apply {
                                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.FAILED.name)
                            }
                            db.update(
                                VoxaDbHelper.TABLE_MESSAGES,
                                failCv,
                                "${VoxaDbHelper.COL_MSG_ID} = ?",
                                arrayOf(msg.id.toString())
                            )
                        }
                    }
                }
                refreshConversations()
                activeConversationId?.let { loadMessages(it) }
            }
        }
    }

    fun receiveIncomingSms(sender: String, messageText: String, timestamp: Long) {
        scope.launch {
            val db = getWritableDb()
            var conv = queryConversationByNumber(sender)
            if (conv == null) {
                val newId = createConversationInternal(db, name = "", number = sender)
                conv = queryConversationById(newId) ?: return@launch
            }

            if (conv.isBlocked) return@launch

            // Insert incoming message
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_CONV_ID, conv.id)
                put(VoxaDbHelper.COL_MSG_DIR, MessageDirection.IN.name)
                put(VoxaDbHelper.COL_MSG_TEXT, messageText)
                put(VoxaDbHelper.COL_MSG_TIME, timestamp)
                put(VoxaDbHelper.COL_MSG_STARRED, 0)
                put(VoxaDbHelper.COL_MSG_STATUS, MessageStatus.DELIVERED.name)
                put(VoxaDbHelper.COL_MSG_TYPE, MessageType.TEXT.name)
            }
            db.insert(VoxaDbHelper.TABLE_MESSAGES, null, cv)

            // Update conversation: un-archive if not keepArchived and not keepArchivedByDefault
            val isCurrentOpen = activeConversationId == conv.id
            val preferences = com.example.voxa.data.preferences.VoxaPreferences(context)
            val keepArchivedDefault = preferences.settings.value.keepArchivedByDefault
            val shouldUnarchive = conv.isArchived && !conv.keepArchived && !keepArchivedDefault

            val convCv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, messageText)
                put(VoxaDbHelper.COL_CONV_LAST_TIME, timestamp)
                put(VoxaDbHelper.COL_CONV_LAST_DIR, "in")
                if (!isCurrentOpen) {
                    put(VoxaDbHelper.COL_CONV_UNREAD, 1)
                }
                if (shouldUnarchive) {
                    put(VoxaDbHelper.COL_CONV_ARCHIVED, 0)
                }
            }
            db.update(
                VoxaDbHelper.TABLE_CONVERSATIONS,
                convCv,
                "${VoxaDbHelper.COL_CONV_ID} = ?",
                arrayOf(conv.id.toString())
            )

            refreshConversations()
            if (isCurrentOpen) {
                loadMessages(conv.id)
            }
        }
    }

    fun togglePin(convId: Long) {
        scope.launch {
            val conv = queryConversationById(convId) ?: return@launch
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_PINNED, if (conv.isPinned) 0 else 1)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun toggleMute(convId: Long) {
        scope.launch {
            val conv = queryConversationById(convId) ?: return@launch
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_MUTED, if (conv.isMuted) 0 else 1)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun toggleUnread(convId: Long) {
        scope.launch {
            val conv = queryConversationById(convId) ?: return@launch
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_UNREAD, if (conv.unread) 0 else 1)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun markAsRead(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_UNREAD, 0)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun markAllAsRead() {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_UNREAD, 0)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, null, null)
            refreshConversations()
        }
    }

    fun archive(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_ARCHIVED, 1)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun restoreFromArchive(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_ARCHIVED, 0)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun toggleKeepArchived(convId: Long) {
        scope.launch {
            val conv = queryConversationById(convId) ?: return@launch
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_KEEP_ARCHIVED, if (conv.keepArchived) 0 else 1)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun moveToBin(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_DELETED, 1)
                put(VoxaDbHelper.COL_CONV_DELETED_AT, System.currentTimeMillis())
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun restoreFromBin(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_DELETED, 0)
                put(VoxaDbHelper.COL_CONV_ARCHIVED, 0)
                putNull(VoxaDbHelper.COL_CONV_DELETED_AT)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun deletePermanently(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            db.delete(VoxaDbHelper.TABLE_MESSAGES, "${VoxaDbHelper.COL_MSG_CONV_ID} = ?", arrayOf(convId.toString()))
            db.delete(VoxaDbHelper.TABLE_CONVERSATIONS, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
            if (activeConversationId == convId) {
                closeConversation()
            }
        }
    }

    fun emptyBin() {
        scope.launch {
            val db = getWritableDb()
            val binList = _conversations.value.filter { it.isDeleted }
            for (c in binList) {
                db.delete(VoxaDbHelper.TABLE_MESSAGES, "${VoxaDbHelper.COL_MSG_CONV_ID} = ?", arrayOf(c.id.toString()))
                db.delete(VoxaDbHelper.TABLE_CONVERSATIONS, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(c.id.toString()))
            }
            refreshConversations()
        }
    }

    fun archiveMultiple(ids: List<Long>) {
        scope.launch {
            val db = getWritableDb()
            for (id in ids) {
                val cv = ContentValues().apply { put(VoxaDbHelper.COL_CONV_ARCHIVED, 1) }
                db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(id.toString()))
            }
            refreshConversations()
        }
    }

    fun moveToBinMultiple(ids: List<Long>) {
        scope.launch {
            val db = getWritableDb()
            val now = System.currentTimeMillis()
            for (id in ids) {
                val cv = ContentValues().apply {
                    put(VoxaDbHelper.COL_CONV_DELETED, 1)
                    put(VoxaDbHelper.COL_CONV_DELETED_AT, now)
                }
                db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(id.toString()))
            }
            refreshConversations()
        }
    }

    fun restoreFromBinMultiple(ids: List<Long>) {
        scope.launch {
            val db = getWritableDb()
            for (id in ids) {
                val cv = ContentValues().apply {
                    put(VoxaDbHelper.COL_CONV_DELETED, 0)
                    put(VoxaDbHelper.COL_CONV_ARCHIVED, 0)
                    putNull(VoxaDbHelper.COL_CONV_DELETED_AT)
                }
                db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(id.toString()))
            }
            refreshConversations()
        }
    }

    fun deletePermanentlyMultiple(ids: List<Long>) {
        scope.launch {
            val db = getWritableDb()
            for (id in ids) {
                db.delete(VoxaDbHelper.TABLE_MESSAGES, "${VoxaDbHelper.COL_MSG_CONV_ID} = ?", arrayOf(id.toString()))
                db.delete(VoxaDbHelper.TABLE_CONVERSATIONS, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(id.toString()))
            }
            refreshConversations()
        }
    }

    fun markMultipleAsRead(ids: List<Long>) {
        scope.launch {
            val db = getWritableDb()
            for (id in ids) {
                val cv = ContentValues().apply { put(VoxaDbHelper.COL_CONV_UNREAD, 0) }
                db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(id.toString()))
            }
            refreshConversations()
        }
    }

    suspend fun searchMessagesContent(query: String): List<Pair<Conversation, Message>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val db = getReadableDb()
        val list = mutableListOf<Pair<Conversation, Message>>()
        val sql = """
            SELECT m.*, c.name, c.number, c.is_archived, c.is_deleted
            FROM ${VoxaDbHelper.TABLE_MESSAGES} m
            JOIN ${VoxaDbHelper.TABLE_CONVERSATIONS} c ON m.${VoxaDbHelper.COL_MSG_CONV_ID} = c.${VoxaDbHelper.COL_CONV_ID}
            WHERE m.${VoxaDbHelper.COL_MSG_TEXT} LIKE ? AND c.${VoxaDbHelper.COL_CONV_DELETED} = 0
            ORDER BY m.${VoxaDbHelper.COL_MSG_TIME} DESC
            LIMIT 50
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf("%$query%"))
        cursor.use {
            while (it.moveToNext()) {
                val msg = it.toMessage()
                val conv = Conversation(
                    id = msg.conversationId,
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    number = it.getString(it.getColumnIndexOrThrow("number")),
                    isArchived = it.getInt(it.getColumnIndexOrThrow("is_archived")) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow("is_deleted")) == 1
                )
                list.add(conv to msg)
            }
        }
        list
    }

    fun purgeOldBinConversations(retentionDays: Int) {
        scope.launch {
            val db = getWritableDb()
            val threshold = System.currentTimeMillis() - (retentionDays * 86400000L)
            db.delete(
                VoxaDbHelper.TABLE_CONVERSATIONS,
                "${VoxaDbHelper.COL_CONV_DELETED} = 1 AND ${VoxaDbHelper.COL_CONV_DELETED_AT} < ?",
                arrayOf(threshold.toString())
            )
            refreshConversations()
        }
    }

    fun toggleStar(msgId: Long) {
        scope.launch {
            val db = getWritableDb()
            val msg = queryMessageById(msgId) ?: return@launch
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_STARRED, if (msg.isStarred) 0 else 1)
            }
            db.update(VoxaDbHelper.TABLE_MESSAGES, cv, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(msgId.toString()))
            activeConversationId?.let { loadMessages(it) }
        }
    }

    fun toggleBlockContact(convId: Long) {
        scope.launch {
            val conv = queryConversationById(convId) ?: return@launch
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_BLOCKED, if (conv.isBlocked) 0 else 1)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun unblockContact(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_BLOCKED, 0)
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
        }
    }

    fun blockNumber(number: String, name: String = "") {
        scope.launch {
            val db = getWritableDb()
            var conv = queryConversationByNumber(number)
            if (conv == null) {
                createConversationInternal(db, name, number)
                conv = queryConversationByNumber(number)
            }
            if (conv != null) {
                val cv = ContentValues().apply {
                    put(VoxaDbHelper.COL_CONV_BLOCKED, 1)
                }
                db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(conv.id.toString()))
            }
            refreshConversations()
        }
    }

    fun cancelScheduledMessage(msgId: Long, convId: Long) {
        deleteMessage(msgId, convId)
    }

    fun deleteMessage(msgId: Long, convId: Long) {
        scope.launch {
            val db = getWritableDb()
            db.delete(VoxaDbHelper.TABLE_MESSAGES, "${VoxaDbHelper.COL_MSG_ID} = ?", arrayOf(msgId.toString()))
            // Update conversation last message if needed
            val remaining = queryMessages(convId)
            val last = remaining.lastOrNull()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, last?.text ?: "")
                put(VoxaDbHelper.COL_CONV_LAST_TIME, last?.time ?: System.currentTimeMillis())
                put(VoxaDbHelper.COL_CONV_LAST_DIR, last?.dir?.name?.lowercase() ?: "out")
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
            if (activeConversationId == convId) {
                loadMessages(convId)
            }
        }
    }

    fun clearConversation(convId: Long) {
        scope.launch {
            val db = getWritableDb()
            db.delete(VoxaDbHelper.TABLE_MESSAGES, "${VoxaDbHelper.COL_MSG_CONV_ID} = ?", arrayOf(convId.toString()))
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_CONV_LAST_TEXT, "")
                put(VoxaDbHelper.COL_CONV_LAST_DIR, "out")
            }
            db.update(VoxaDbHelper.TABLE_CONVERSATIONS, cv, "${VoxaDbHelper.COL_CONV_ID} = ?", arrayOf(convId.toString()))
            refreshConversations()
            if (activeConversationId == convId) {
                loadMessages(convId)
            }
        }
    }

    fun createConversation(name: String, number: String): Long {
        val existing = _conversations.value.firstOrNull { it.number == number.trim() }
        if (existing != null) {
            return existing.id
        }
        val db = getWritableDb()
        val id = createConversationInternal(db, name.trim(), number.trim())
        refreshConversations()
        return id
    }

    private fun createConversationInternal(db: SQLiteDatabase, name: String, number: String): Long {
        val cv = ContentValues().apply {
            put(VoxaDbHelper.COL_CONV_NAME, name)
            put(VoxaDbHelper.COL_CONV_NUMBER, number)
            put(VoxaDbHelper.COL_CONV_PINNED, 0)
            put(VoxaDbHelper.COL_CONV_MUTED, 0)
            put(VoxaDbHelper.COL_CONV_ARCHIVED, 0)
            put(VoxaDbHelper.COL_CONV_DELETED, 0)
            put(VoxaDbHelper.COL_CONV_KEEP_ARCHIVED, 0)
            put(VoxaDbHelper.COL_CONV_UNREAD, 0)
            put(VoxaDbHelper.COL_CONV_BLOCKED, 0)
            put(VoxaDbHelper.COL_CONV_LAST_TEXT, "")
            put(VoxaDbHelper.COL_CONV_LAST_TIME, System.currentTimeMillis())
            put(VoxaDbHelper.COL_CONV_LAST_DIR, "out")
        }
        return db.insert(VoxaDbHelper.TABLE_CONVERSATIONS, null, cv)
    }

    private fun queryConversationById(id: Long): Conversation? {
        val db = getReadableDb()
        val cursor = db.query(
            VoxaDbHelper.TABLE_CONVERSATIONS,
            null,
            "${VoxaDbHelper.COL_CONV_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.toConversation()
            }
        }
        return null
    }

    private fun queryConversationByNumber(number: String): Conversation? {
        val db = getReadableDb()
        val cursor = db.query(
            VoxaDbHelper.TABLE_CONVERSATIONS,
            null,
            "${VoxaDbHelper.COL_CONV_NUMBER} = ?",
            arrayOf(number),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.toConversation()
            }
        }
        return null
    }

    private fun queryMessageById(id: Long): Message? {
        val db = getReadableDb()
        val cursor = db.query(
            VoxaDbHelper.TABLE_MESSAGES,
            null,
            "${VoxaDbHelper.COL_MSG_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.toMessage()
            }
        }
        return null
    }

    private fun Cursor.toConversation(): Conversation = Conversation(
        id = getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_ID)),
        name = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_NAME)),
        number = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_NUMBER)),
        isPinned = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_PINNED)) == 1,
        isMuted = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_MUTED)) == 1,
        isArchived = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_ARCHIVED)) == 1,
        isDeleted = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_DELETED)) == 1,
        deletedAt = if (isNull(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_DELETED_AT))) null else getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_DELETED_AT)),
        keepArchived = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_KEEP_ARCHIVED)) == 1,
        unread = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_UNREAD)) == 1,
        isBlocked = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_BLOCKED)) == 1,
        lastMessageText = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_LAST_TEXT)),
        lastMessageTime = getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_LAST_TIME)),
        lastMessageDir = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_CONV_LAST_DIR))
    )

    private fun Cursor.toMessage(): Message = Message(
        id = getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_ID)),
        conversationId = getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_CONV_ID)),
        dir = runCatching { MessageDirection.valueOf(getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_DIR))) }.getOrDefault(MessageDirection.IN),
        text = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_TEXT)),
        time = getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_TIME)),
        isStarred = getInt(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_STARRED)) == 1,
        status = runCatching {
            val s = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_STATUS))
            if (s == "PENDING") MessageStatus.SENDING else MessageStatus.valueOf(s)
        }.getOrDefault(MessageStatus.SENT),
        scheduledTime = if (isNull(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_SCHEDULED_TIME))) null else getLong(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_SCHEDULED_TIME)),
        type = runCatching { MessageType.valueOf(getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_TYPE))) }.getOrDefault(MessageType.TEXT),
        dataUri = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_DATA_URI)),
        fileName = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_FILE_NAME)),
        fileSize = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_FILE_SIZE)),
        contactName = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_CONTACT_NAME)),
        contactNumber = getString(getColumnIndexOrThrow(VoxaDbHelper.COL_MSG_CONTACT_NUMBER))
    )

    fun updateMessageStatus(msgId: Long, status: MessageStatus) {
        scope.launch {
            val db = getWritableDb()
            val cv = ContentValues().apply {
                put(VoxaDbHelper.COL_MSG_STATUS, status.name)
            }
            db.update(
                VoxaDbHelper.TABLE_MESSAGES,
                cv,
                "${VoxaDbHelper.COL_MSG_ID} = ?",
                arrayOf(msgId.toString())
            )
            activeConversationId?.let { loadMessages(it) }
        }
    }

    suspend fun syncDeviceSms(): Int = withContext(Dispatchers.IO) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            return@withContext 0
        }

        try {
            val contentResolver = context.contentResolver
            val uri = Uri.parse("content://sms")
            val projection = arrayOf("_id", "thread_id", "address", "body", "date", "type", "read", "status")
            val cursor = contentResolver.query(uri, projection, null, null, "date DESC") ?: return@withContext 0

            var importedCount = 0
            val db = getWritableDb()

            cursor.use { c ->
                val addrCol = c.getColumnIndex("address")
                val bodyCol = c.getColumnIndex("body")
                val dateCol = c.getColumnIndex("date")
                val typeCol = c.getColumnIndex("type")
                val readCol = c.getColumnIndex("read")

                while (c.moveToNext()) {
                    val rawAddress = if (addrCol >= 0) c.getString(addrCol) else null
                    if (rawAddress.isNullOrBlank()) continue
                    val address = rawAddress.trim()

                    val body = if (bodyCol >= 0) c.getString(bodyCol) ?: "" else ""
                    val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()
                    val type = if (typeCol >= 0) c.getInt(typeCol) else 1 // 1=INBOX, 2=SENT
                    val isRead = if (readCol >= 0) c.getInt(readCol) == 1 else true

                    val dir = if (type == 2) MessageDirection.OUT else MessageDirection.IN
                    val msgStatus = when {
                        type == 2 && isRead -> MessageStatus.READ
                        type == 2 -> MessageStatus.DELIVERED
                        else -> MessageStatus.DELIVERED
                    }

                    val existingConv = queryConversationByNumber(address)
                    val convId: Long
                    if (existingConv == null) {
                        val contactName = resolveContactName(address)
                        val cvConv = ContentValues().apply {
                            put(VoxaDbHelper.COL_CONV_NAME, contactName ?: "")
                            put(VoxaDbHelper.COL_CONV_NUMBER, address)
                            put(VoxaDbHelper.COL_CONV_PINNED, 0)
                            put(VoxaDbHelper.COL_CONV_MUTED, 0)
                            put(VoxaDbHelper.COL_CONV_ARCHIVED, 0)
                            put(VoxaDbHelper.COL_CONV_DELETED, 0)
                            put(VoxaDbHelper.COL_CONV_UNREAD, if (!isRead && dir == MessageDirection.IN) 1 else 0)
                            put(VoxaDbHelper.COL_CONV_LAST_TEXT, body)
                            put(VoxaDbHelper.COL_CONV_LAST_TIME, date)
                            put(VoxaDbHelper.COL_CONV_LAST_DIR, dir.name.lowercase())
                        }
                        convId = db.insert(VoxaDbHelper.TABLE_CONVERSATIONS, null, cvConv)
                    } else {
                        convId = existingConv.id
                        if (date > existingConv.lastMessageTime) {
                            val cvUpdate = ContentValues().apply {
                                put(VoxaDbHelper.COL_CONV_LAST_TEXT, body)
                                put(VoxaDbHelper.COL_CONV_LAST_TIME, date)
                                put(VoxaDbHelper.COL_CONV_LAST_DIR, dir.name.lowercase())
                                if (!isRead && dir == MessageDirection.IN) {
                                    put(VoxaDbHelper.COL_CONV_UNREAD, 1)
                                }
                            }
                            db.update(
                                VoxaDbHelper.TABLE_CONVERSATIONS,
                                cvUpdate,
                                "${VoxaDbHelper.COL_CONV_ID} = ?",
                                arrayOf(convId.toString())
                            )
                        }
                    }

                    // Check if message already exists
                    val checkCursor = db.query(
                        VoxaDbHelper.TABLE_MESSAGES,
                        arrayOf(VoxaDbHelper.COL_MSG_ID),
                        "${VoxaDbHelper.COL_MSG_CONV_ID} = ? AND ${VoxaDbHelper.COL_MSG_TIME} = ?",
                        arrayOf(convId.toString(), date.toString()),
                        null, null, null
                    )
                    val exists = checkCursor.use { it.moveToFirst() }

                    if (!exists) {
                        val cvMsg = ContentValues().apply {
                            put(VoxaDbHelper.COL_MSG_CONV_ID, convId)
                            put(VoxaDbHelper.COL_MSG_DIR, dir.name)
                            put(VoxaDbHelper.COL_MSG_TEXT, body)
                            put(VoxaDbHelper.COL_MSG_TIME, date)
                            put(VoxaDbHelper.COL_MSG_STARRED, 0)
                            put(VoxaDbHelper.COL_MSG_STATUS, msgStatus.name)
                            put(VoxaDbHelper.COL_MSG_TYPE, MessageType.TEXT.name)
                        }
                        db.insert(VoxaDbHelper.TABLE_MESSAGES, null, cvMsg)
                        importedCount++
                    }
                }
            }

            if (importedCount > 0) {
                refreshConversations()
                activeConversationId?.let { loadMessages(it) }
            }
            importedCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    private fun resolveContactName(phoneNumber: String): String? {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permission != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: VoxaRepository? = null

        fun getInstance(context: Context): VoxaRepository =
            instance ?: synchronized(this) {
                instance ?: VoxaRepository(context.applicationContext).also { instance = it }
            }
    }
}
