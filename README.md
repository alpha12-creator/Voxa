# VOXA — SMS & Messaging App

**Tagline:** Private. Simple. Yours.

## What's in this package

- `index.html` — a self-contained, interactive UI prototype of VOXA (Messages, Archive, Bin,
  Settings, conversation threads with emoji, scheduled messages, and photo/file/contact
  attachments). Pure HTML/CSS/JS, no build step, no dependencies. Open it directly in a browser
  or serve it as a static page.
- `voxa-spec.md` — the original V1 product/feature specification this prototype was built from.

## Important scope note

This prototype demonstrates the UI, navigation, and interaction design only. It runs entirely in
the browser using mock data and `localStorage` — it does **not** send or receive real SMS, does
not integrate with Android's SMS/Telephony APIs, and cannot be registered as a default SMS app.
Real SMS functionality (send/receive, default-SMS-app role, background services, dual-SIM
selection, contact/permission access) requires a native Android build (Kotlin/Java + Android SDK).

## Suggested next step

Use `index.html` as the visual/UX reference and `voxa-spec.md` as the functional spec when
prompting an AI app-builder (e.g. Google AI Studio) or a native Android developer to implement
the real app with the Android SMS APIs.
