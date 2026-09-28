# VOXA — Android SMS & Messaging App

**Tagline:** Private. Simple. Yours.

VOXA is a professional, lightweight, native Android SMS messaging application built with Kotlin and Jetpack Compose. Engineered for high performance, modern privacy-focused design, and optimized for smooth operation even on low-end and Android Go devices.

## Features

- **Full SMS & Messaging**:
  - Send and receive carrier SMS messages with delivery status tracking.
  - Native integration with Android Telephony system and SMS Broadcast Receivers.
  - Direct Phone Call action (`📞`) in thread top bar to dial contacts immediately.
  - Setup flow for setting VOXA as the system default SMS application.
  - Smart Quick Replies chips above the composer (`👍 Okay`, `On my way!`, `Can't talk right now`, etc.).
  - Voice audio notes: dynamic microphone button when text is empty to record and send voice messages with waveforms.
  - Message scheduling (send automatically at a chosen future date and time).
  - Rich attachments support: Photos, documents, and contact cards.
  - Built-in emoji picker for fast reactions and expressive conversations.
  - In-chat search to search for specific terms within an open conversation.
  - Message action menu on tap/long-press: Copy text, Star/Unstar, Share/Forward, Message Details, and Delete message.
  - Contact Details Sheet: tap contact header to view phone number, dial call, copy number, mute, block, or clear chat history.

- **Inbox & Three-Dots Navigation (No Bottom Bar)**:
  - Full-screen messages layout without bottom tabs, mirroring Google Messages and Samsung Messages.
  - Top-right **Three-Dots Menu (`⋮`)** provides seamless navigation:
    - 📁 **Archived** (with count badge)
    - 🚫 **Spam & Blocked** (with count badge)
    - ⭐ **Starred messages**
    - 🗑️ **Bin / Trash** (with count badge)
    - ✓✓ **Mark all as read**
    - ⚙️ **Settings**
  - All sub-screens include a dedicated top app bar with Back button (`←`) and hardware/gesture Back navigation.
  - Category Filter Chips at the top of the inbox: **All**, **Personal**, **OTP / Codes**, **Transactions**, **Unread**.
  - Pin important conversations to the top.
  - Mute noisy threads and mark conversations as read or unread.

- **Spam & Blocked Protection**:
  - Dedicated Spam & Blocked view with one-tap unblock and add blocked numbers.

- **Archive System**:
  - First-class Archive section to declutter the inbox without losing chat history.
  - Optional "Keep archived" toggle to prevent incoming messages from un-archiving threads automatically.

- **Bin / Trash Management**:
  - Safe two-step deletion: conversations move to Bin before permanent removal.
  - Configurable auto-delete retention period (default 30 days).
  - One-tap "Empty Bin" with confirmation dialog.

- **Search**:
  - Instant global search across contact names, phone numbers, and message contents.

- **Customization & Themes**:
  - Dark Mode (default), Light Mode, and System Theme support.
  - 5 customizable accent colors: Electric Blue, Emerald Green, Neon Purple, Vibrant Orange, and Hot Pink.
  - Notification controls with an option to hide message previews for enhanced privacy.

## Architecture

- **Language:** Kotlin 2.2
- **UI Framework:** Jetpack Compose with Material Design 3
- **Architecture:** Clean MVVM with Repository pattern
- **Database:** SQLite local persistent storage with reactive StateFlow streams
- **Android Target:** SDK 36 (Android 16), minSdk 24
- **Performance:** Optimized for minimal RAM usage, fast cold startup, and zero battery drain.
