package com.example.voxa.data.model

enum class MessageDirection {
    IN, OUT
}

enum class MessageStatus {
    SENDING, PENDING, SENT, DELIVERED, READ, FAILED, SCHEDULED
}

enum class MessageType {
    TEXT, IMAGE, FILE, CONTACT, VOICE
}

data class Message(
    val id: Long = 0,
    val conversationId: Long,
    val dir: MessageDirection = MessageDirection.OUT,
    val text: String = "",
    val time: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val scheduledTime: Long? = null,
    val type: MessageType = MessageType.TEXT,
    val dataUri: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val contactName: String? = null,
    val contactNumber: String? = null
)
