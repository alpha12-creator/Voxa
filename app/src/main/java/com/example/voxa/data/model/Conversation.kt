package com.example.voxa.data.model

data class Conversation(
    val id: Long = 0,
    val name: String = "",
    val number: String,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val keepArchived: Boolean = false,
    val unread: Boolean = false,
    val isBlocked: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessageDir: String = "in"
) {
    val displayName: String
        get() = name.ifBlank { number }

    val initials: String
        get() {
            val src = displayName.trim()
            if (src.isEmpty()) return "?"
            val parts = src.split(" ").filter { it.isNotEmpty() }
            return when {
                parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                parts.isNotEmpty() -> "${parts[0].first()}".uppercase()
                else -> "?"
            }
        }
}
