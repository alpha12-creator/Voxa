# VOXA — V1 SMS & Messaging App — Product Spec

You are an expert Android app designer and developer.

Build a professional, lightweight SMS messaging application called VOXA.

The goal is to create a clean, modern, reliable messaging app that can work smoothly on low-end Android devices, including Android Go devices.

Do NOT overcomplicate the first version. Build only the features specified below. The app should be structured so additional features can be added later.

---

## 1. App Identity

- App name: VOXA
- Category: SMS / Messaging
- Tagline: Private. Simple. Yours.

Brand personality: Minimal, Premium, Fast, Clean, Private, Modern.

VOXA should not look like a cheap utility app. It should feel like a polished commercial messaging application while remaining lightweight.

---

## 2. Design System

Use a modern Material-style interface with custom VOXA branding.

**Default theme:** Dark theme.

Dark colors:
- Background: deep charcoal / near-black
- Surface: slightly lighter charcoal
- Primary accent: electric blue
- Primary text: white
- Secondary text: soft gray
- Dividers: subtle dark gray

Do not use excessive gradients, excessive shadows, or unnecessary animations.

**Light theme:** White/off-white background, dark text, VOXA electric-blue accent.

**Accent colors:** Allow the user to select an accent color from a small predefined palette. Keep the interface visually consistent when the accent color changes.

---

## 3. App Navigation

Bottom navigation: Messages, Archive, Bin, Settings. Simple and easy to understand.

---

## 4. Messages / Inbox

Main screen. Top: "VOXA" + Search button + More/options button.

Each conversation shows: contact avatar or initial, contact name (or phone number if no name), latest message preview, time/date, unread indicator.

Conversation actions: open, pin, mark as read/unread, mute, archive, delete, block contact. Use intuitive swipe gestures where appropriate, but don't hide important actions.

---

## 5. Message Conversation Screen

Top bar: back button, contact name, contact number where appropriate, more options.

Bubbles clearly distinguish incoming vs outgoing SMS.

Bottom: text input + send button. Support SMS sending/receiving, timestamps, delivery/status info when supported by the underlying Android SMS system. Don't pretend features are supported when the device/network doesn't support them.

---

## 6. Archive

Archiving hides a conversation from the main inbox without deleting it — messages remain intact, it can be reopened from Archive, and restored to inbox.

"Keep archived" option: when enabled, new incoming messages should not automatically return the conversation to the main inbox, where Android's SMS APIs and system behavior allow this. Clearly handle Android limitations. Archive is a first-class feature, not a hidden delete option.

---

## 7. Bin / Trash

Deleting a conversation moves it to Bin instead of permanently deleting it. Inside Bin: Restore, Delete permanently. Provide "Empty Bin" with confirmation.

Automatic deletion: configurable retention period, default 30 days, after which conversations may be permanently deleted. Never permanently delete without confirmation unless the user has explicitly enabled auto-deletion.

---

## 8. Starred Messages

Mark individual messages as ⭐ Starred. Provide a Starred section/filter. A starred message stays starred even if its conversation is archived. Deleting a conversation should handle its starred messages consistently with the Bin/restore system.

---

## 9. Mute

Mute individual conversations. Muted conversations stay visible normally unless archived, but notifications follow the mute setting. Simple unmute option.

---

## 10. Block Contact

Block a sender/contact where Android permissions and SMS APIs support it. Clear confirmation before blocking. Don't claim to block at the carrier/network level — explain system limitations.

---

## 11. Search

Fast global search across contact names, phone numbers, and message text. Results clearly show matching conversation, relevant message preview, date/time. Keep it lightweight and fast.

---

## 12. Settings

Sections: Appearance (dark/light/system default, accent color), Messages (default SMS behavior, conversation preferences, send options), Notifications (enable/disable, sound, vibration, preview/privacy), Archive (keep archived, archive behavior), Bin (auto-delete period, empty bin), Privacy (basic privacy options, hide message previews from notifications), About (VOXA, version, tagline).

---

## 13. Default SMS App

Design VOXA to work as a proper Android SMS application, including the flow for the user to set it as the default SMS app. Don't bypass Android's security model. Request only genuinely required permissions and explain them clearly. Follow Android's requirements for default SMS apps.

---

## 14. Android Go / Low-End Optimization

Critical. Prioritize: low RAM usage, fast startup, small APK size, efficient lists and DB queries, minimal background processing, minimal animations, no unnecessary services/network requests/heavy graphics/libraries. Must stay responsive with hundreds or thousands of SMS messages. Use pagination/lazy loading; avoid loading the entire message database into memory.

---

## 15. Offline-First Behavior

Basic SMS functionality should work without internet access. No online account required. No cloud sync in V1. SMS data stays locally managed by Android and the app.

---

## 16. Privacy

No account creation, email registration, social login, or cloud account required. Don't collect unnecessary personal information. Don't send SMS content to external servers. Keep the app local-first.

---

## 17. UI Details

Use: rounded but restrained cards, clean typography, consistent spacing, simple icons, smooth but minimal transitions, clear empty states, clear confirmation dialogs, proper dark-mode contrast.

Avoid: excessive glassmorphism, excessive gradients, huge buttons, cluttered screens, too many colors, unnecessary animations. VOXA should look premium because of spacing, typography, hierarchy and simplicity — not visual effects.

---

## 18. Empty States

- No messages: "Your conversations will appear here."
- Archive empty: "No archived conversations."
- Bin empty: "Your bin is empty."
- Starred empty: "No starred messages yet."

Keep these simple and friendly.

---

## 19. Error Handling

Handle gracefully: SMS cannot be sent, permission denied, VOXA not the default SMS app, SIM unavailable, dual-SIM selection, message failed, contact unavailable, database/loading errors. Show understandable messages instead of technical errors.

---

## 20. Dual SIM

Where supported, let the user pick which SIM sends an SMS. If the device has only one SIM, don't show SIM-selection UI.

---

## 21. Accessibility

Support: readable text, adequate touch targets, good contrast, screen-reader-friendly labels, scalable text where practical.

---

## 22. V1 Scope — Explicitly Excluded

AI assistant, cloud backup, user accounts, social messaging, WhatsApp integration, Telegram integration, RCS replacement, cryptocurrency, advertising system, complicated automation, subscription system. These can be considered for future versions.

---

## 23. Project Structure

Maintainable architecture, separated into: UI, Data, SMS handling, Database/storage, Settings, Notifications, Archive system, Bin system. Clear naming, reusable components, easy to extend.

---

## 24. Important Product Principle

VOXA is not trying to have every feature imaginable.

> Simple enough to be fast.
> Powerful enough to replace the default SMS app.
> Clean enough to enjoy using every day.

Build the core experience first. Do not add features that were not requested.

---

## 25. Deliverable Checklist

1. Test main navigation.
2. Test sending and receiving SMS.
3. Test archive/restore.
4. Test delete → Bin → Restore.
5. Test permanent deletion.
6. Test search.
7. Test pin/mute/star.
8. Test notification behavior.
9. Test default SMS app flow.
10. Test dark/light themes.
11. Test on a low-end Android/Android Go environment if available.
12. Check for crashes and unnecessary memory usage.

The final result should feel like a real, polished messaging application — not a prototype.

**App name:** VOXA
**Tagline:** Private. Simple. Yours.
