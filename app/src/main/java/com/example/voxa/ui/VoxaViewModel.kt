package com.example.voxa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voxa.data.model.Conversation
import com.example.voxa.data.model.Message
import com.example.voxa.data.model.MessageStatus
import com.example.voxa.data.model.MessageType
import com.example.voxa.data.preferences.AccentColor
import com.example.voxa.data.preferences.ThemeMode
import com.example.voxa.data.preferences.VoxaPreferences
import com.example.voxa.data.preferences.VoxaSettings
import com.example.voxa.data.repository.VoxaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VoxaTab {
    MESSAGES, ARCHIVE, BIN, STARRED, SPAM_BLOCKED, SETTINGS
}

enum class CategoryFilter(val label: String) {
    ALL("All"),
    PERSONAL("Personal"),
    OTP("OTP / Codes"),
    TRANSACTIONS("Transactions"),
    UNREAD("Unread")
}

class VoxaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VoxaRepository.getInstance(application)
    private val preferences = VoxaPreferences(application)

    val settings: StateFlow<VoxaSettings> = preferences.settings

    private val _currentTab = MutableStateFlow(VoxaTab.MESSAGES)
    val currentTab: StateFlow<VoxaTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    val selectedCategory: StateFlow<CategoryFilter> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _openConversationId = MutableStateFlow<Long?>(null)
    val openConversationId: StateFlow<Long?> = _openConversationId.asStateFlow()

    private val _starredMessages = MutableStateFlow<List<Pair<Conversation, Message>>>(emptyList())
    val starredMessages: StateFlow<List<Pair<Conversation, Message>>> = _starredMessages.asStateFlow()

    val currentMessages: StateFlow<List<Message>> = repository.currentMessages

    // Inbox conversations filtered by search & category
    val inboxConversations: StateFlow<List<Conversation>> = combine(
        repository.conversations,
        _searchQuery,
        _selectedCategory
    ) { convs, query, category ->
        convs.filter { !it.isArchived && !it.isDeleted && !it.isBlocked }
            .filter { matchesQuery(it, query) }
            .filter { matchesCategory(it, category) }
            .sortedWith(
                compareByDescending<Conversation> { it.isPinned }
                    .thenByDescending { it.lastMessageTime }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Archive conversations
    val archiveConversations: StateFlow<List<Conversation>> = combine(
        repository.conversations,
        _searchQuery
    ) { convs, query ->
        convs.filter { it.isArchived && !it.isDeleted }
            .filter { matchesQuery(it, query) }
            .sortedByDescending { it.lastMessageTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bin conversations
    val binConversations: StateFlow<List<Conversation>> = combine(
        repository.conversations,
        _searchQuery
    ) { convs, query ->
        convs.filter { it.isDeleted }
            .filter { matchesQuery(it, query) }
            .sortedByDescending { it.lastMessageTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Blocked conversations
    val blockedConversations: StateFlow<List<Conversation>> = combine(
        repository.conversations,
        _searchQuery
    ) { convs, query ->
        convs.filter { it.isBlocked }
            .filter { matchesQuery(it, query) }
            .sortedByDescending { it.lastMessageTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stats and Counts for badges in three-dots menu
    val archiveCount: StateFlow<Int> = repository.conversations.map { list ->
        list.count { it.isArchived && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val binCount: StateFlow<Int> = repository.conversations.map { list ->
        list.count { it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val blockedCount: StateFlow<Int> = repository.conversations.map { list ->
        list.count { it.isBlocked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Automatically sync real device SMS messages on launch
        viewModelScope.launch {
            repository.syncDeviceSms()
        }

        // Scheduled message ticker and bin retention worker
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                repository.checkScheduledMessages()
            }
        }

        viewModelScope.launch {
            repository.purgeOldBinConversations(settings.value.binRetentionDays)
        }
    }

    private fun matchesQuery(conv: Conversation, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        val digitsOnly = q.filter { it.isDigit() }
        val convDigits = conv.number.filter { it.isDigit() }

        val matchesNumber = conv.number.lowercase().contains(q) ||
                (digitsOnly.isNotEmpty() && convDigits.contains(digitsOnly))
        val matchesName = conv.name.lowercase().contains(q)
        val matchesLastMessage = conv.lastMessageText.lowercase().contains(q)

        return matchesName || matchesNumber || matchesLastMessage
    }

    private fun matchesCategory(conv: Conversation, category: CategoryFilter): Boolean {
        val text = conv.lastMessageText.lowercase()
        return when (category) {
            CategoryFilter.ALL -> true
            CategoryFilter.UNREAD -> conv.unread
            CategoryFilter.OTP -> {
                text.contains("otp") || text.contains("code") || text.contains("verification") ||
                        text.contains("pin") || text.contains("namba ya siri") ||
                        (conv.name.isBlank() && conv.number.length < 8)
            }
            CategoryFilter.TRANSACTIONS -> {
                text.contains("sent") || text.contains("received") || text.contains("paid") ||
                        text.contains("balance") || text.contains("bank") || text.contains("tzs") ||
                        text.contains("$") || text.contains("salio") || text.contains("umepokea") ||
                        text.contains("payment") || text.contains("account") || text.contains("transfer")
            }
            CategoryFilter.PERSONAL -> {
                !matchesCategory(conv, CategoryFilter.OTP) && !matchesCategory(conv, CategoryFilter.TRANSACTIONS)
            }
        }
    }

    fun setCategory(category: CategoryFilter) {
        _selectedCategory.value = category
    }

    fun setTab(tab: VoxaTab) {
        _currentTab.value = tab
        _openConversationId.value = null
        _isSearchVisible.value = false
        _searchQuery.value = ""
        repository.closeConversation()
        if (tab == VoxaTab.STARRED) {
            loadStarredMessages()
        }
    }

    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openConversation(id: Long) {
        _openConversationId.value = id
        repository.openConversation(id)
    }

    fun closeConversation() {
        _openConversationId.value = null
        repository.closeConversation()
    }

    fun getConversation(id: Long): Conversation? {
        return repository.conversations.value.firstOrNull { it.id == id }
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
        repository.sendMessage(convId, text, type, dataUri, fileName, fileSize, contactName, contactNumber)
    }

    fun scheduleMessage(convId: Long, text: String, scheduledTime: Long) {
        repository.scheduleMessage(convId, text, scheduledTime)
    }

    fun startNewConversation(name: String, number: String) {
        val id = repository.createConversation(name, number)
        openConversation(id)
    }

    fun togglePin(id: Long) = repository.togglePin(id)
    fun toggleMute(id: Long) = repository.toggleMute(id)
    fun toggleUnread(id: Long) = repository.toggleUnread(id)
    fun archive(id: Long) = repository.archive(id)
    fun restoreFromArchive(id: Long) = repository.restoreFromArchive(id)
    fun toggleKeepArchived(id: Long) = repository.toggleKeepArchived(id)
    fun moveToBin(id: Long) = repository.moveToBin(id)
    fun restoreFromBin(id: Long) = repository.restoreFromBin(id)
    fun deletePermanently(id: Long) = repository.deletePermanently(id)
    fun emptyBin() = repository.emptyBin()
    fun toggleStar(msgId: Long) = repository.toggleStar(msgId)
    fun toggleBlock(id: Long) = repository.toggleBlockContact(id)
    fun unblockContact(id: Long) = repository.unblockContact(id)
    fun blockNumber(number: String, name: String = "") = repository.blockNumber(number, name)
    fun deleteMessage(msgId: Long, convId: Long) = repository.deleteMessage(msgId, convId)
    fun clearConversation(convId: Long) = repository.clearConversation(convId)
    fun markAllAsRead() = repository.markAllAsRead()

    fun syncDeviceMessages() {
        viewModelScope.launch {
            repository.syncDeviceSms()
        }
    }

    fun markMessageStatus(msgId: Long, status: MessageStatus) {
        viewModelScope.launch {
            repository.updateMessageStatus(msgId, status)
        }
    }

    fun loadStarredMessages() {
        viewModelScope.launch {
            _starredMessages.value = repository.getStarredMessages()
        }
    }

    // Settings
    fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    fun setAccentColor(accent: AccentColor) = preferences.setAccentColor(accent)
    fun setKeepArchivedByDefault(enabled: Boolean) = preferences.setKeepArchivedByDefault(enabled)
    fun setBinRetentionDays(days: Int) = preferences.setBinRetentionDays(days)
    fun setNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)
    fun setHideMessagePreview(enabled: Boolean) = preferences.setHideMessagePreview(enabled)
}
