# Call Assistant — Complete Feature Guide

This guide describes the current implementation of the Call Assistant Android app, organized by feature area and keyed to the actual code files.

---

## 1. App Architecture & Entry Point

- **Single-activity Compose app** driven by `MainActivity.kt`.
- `MainActivity` requests runtime permissions, then prompts the user to set Call Assistant as:
  - Default dialer (`TelecomManager.ACTION_CHANGE_DEFAULT_DIALER`)
  - Default SMS app (`RoleManager.ROLE_SMS` on Q+, legacy intent on older)
  - Call-screening role (`RoleManager.ROLE_CALL_SCREENING` on Q+)
- The main UI is `MainApp.kt`, which provides the top app bar, bottom navigation bar, and a `MainViewModel` shared across all screens.

**Key files:**
- `app/src/main/java/com/callassistant/MainActivity.kt`
- `app/src/main/java/com/callassistant/ui/MainApp.kt`
- `app/src/main/java/com/callassistant/ui/MainViewModel.kt`

---

## 2. Permissions

The app requests granular permissions for contacts, call log, SMS, phone, and audio. Each screen is wrapped in `PermissionGuard`, which shows a permission rationale and a request button.

**Key files:**
- `app/src/main/java/com/callassistant/permission/RequiredPermission.kt`
- `app/src/main/java/com/callassistant/ui/components/PermissionGuard.kt`

---

## 3. Local Database (Room)

The app caches data in a Room database, `AppDatabase`, named **`call_assistant_db`**.

**Entities:**
- `Contact` (`contacts`) — name, phone, source (`LOCAL` / `IMPORTED`), photo URI, address, latitude, longitude
- `CallLogEntry` (`call_logs`) — number, name, type (`INCOMING` / `OUTGOING` / `MISSED`), timestamp, duration, blocked flag
- `SmsMessage` (`sms_messages`) — number, name, body, timestamp, direction (`IN` / `OUT`)
- `SpamRule` (`spam_rules`) — pattern, type (`REGEX` / `EXACT` / `PREFIX`), label, blocking flag
- `BlockedNumber` (`blocked_numbers`) — number, name, reason, timestamp, attempt count, last attempt

**Key files:**
- `app/src/main/java/com/callassistant/data/db/AppDatabase.kt`
- `app/src/main/java/com/callassistant/data/entity/*.kt`
- `docs/DATABASE_ACCESS.md` (already in the repo)

---

## 4. Contacts

`ContactsScreen` shows the local contact list.

**Features:**
- **Search** by name or phone number.
- **Sort** by Name A-Z, Name Z-A, Newest, Oldest, and Most used.
- **Multi-select** with long-press; bulk delete or block selected contacts.
- **Per-contact actions:** Edit, Call, Message, WhatsApp, IMO.
- **Avatar preview:** `ContactAvatar` loads `photoUri` from the gallery if present, otherwise falls back to a colored initial.
- **Add/Edit dialog:** `AddContactDialog` supports both creating a new contact and editing an existing one, including photo selection, address, and a map picker for location.
- `MainViewModel.saveContact()` uses `ContactDao.insertAll()` with `OnConflictStrategy.REPLACE`, so editing reuses the existing `id`.

**Sync behavior:** `ContactSyncer` reads `ContactsContract.CommonDataKinds.Phone.CONTENT_URI` and `MainViewModel` refreshes the table. Deleting a contact also removes it from the system `ContactsContract` database.

**Key files:**
- `app/src/main/java/com/callassistant/ui/screens/ContactsScreen.kt`
- `app/src/main/java/com/callassistant/data/sync/ContactSyncer.kt`

---

## 5. Call Log

`CallLogScreen` displays call history.

**Features:**
- Groups calls by number and day category (**Today / Yesterday / Older**).
- **Search** and **sort** (Newest, Oldest, Name A-Z, Name Z-A).
- **Expandable groups** for numbers with multiple calls.
- **Multi-select** for delete or block.
- Pagination of the **Older** section (loads 20 at a time as you scroll).
- Sync FAB refreshes from the system `CallLog.Calls` provider.
- `CallLogSyncer` pulls actual Android call logs into Room.

**Key files:**
- `app/src/main/java/com/callassistant/ui/screens/CallLogScreen.kt`
- `app/src/main/java/com/callassistant/data/sync/CallLogSyncer.kt`

---

## 6. Dial Pad

`DialPadScreen` is a custom dialer with T9 smart search.

**Features:**
- Numeric keypad with long-press handling for `0` and `#`.
- **T9 smart-dial search** with ~150ms debounce.
- Matches entered digits against normalized phone numbers and T9-encoded contact names (both full words and initials).
- Ranks suggestions by call frequency and recency.
- Highlights matched parts of the name/number in the suggestion list.
- Direct call, message, WhatsApp, and IMO actions on each suggestion.
- "No results" state shown when nothing matches.

**Key files:**
- `app/src/main/java/com/callassistant/ui/screens/DialPadScreen.kt`

---

## 7. SMS / Messages

`MessagesScreen` shows message threads.

**Features:**
- Threads grouped by phone number.
- Search and sort (Newest, Oldest, Name A-Z, Name Z-A).
- Multi-select delete.
- Opens `MessageThreadScreen` for the full conversation.
- Floating action button opens `NewMessageScreen` to compose a message to one or more contacts.

`MessageThreadScreen` displays a WhatsApp-style bubble list, auto-scrolls to the newest message, and sends via `SmsManager` while persisting an `SmsDirection.OUT` entry.

`SmsReceiver` intercepts incoming SMS, checks blocked numbers and spam rules, aborts the broadcast for blocked senders, and inserts valid messages into the local database.

**Key files:**
- `app/src/main/java/com/callassistant/ui/screens/MessagesScreen.kt`
- `app/src/main/java/com/callassistant/ui/screens/MessageThreadScreen.kt`
- `app/src/main/java/com/callassistant/receiver/SmsReceiver.kt`
- `app/src/main/java/com/callassistant/data/sync/SmsSyncer.kt`

---

## 8. Spam & Blocking

`SpamRulesScreen` has two tabs: **Rules** and **Blocked Numbers**.

**Features:**
- View/delete spam rules.
- Add a rule with a pattern, type (REGEX / EXACT / PREFIX), label, and toggle for **blocking** vs. **flag-only**.
- View blocked numbers, including reason and call-attempt count.
- Manually block a number with an optional name/reason.
- Unblock numbers.

`SpamRuleRepository` is the single source of truth for whether a number is blocked. On an incoming call, `CallScreeningServiceImpl` rejects blocked callers, marks the call as blocked in the call log, and records the attempt. On an incoming SMS, `SmsReceiver` aborts the broadcast if the sender is blocked.

`MainViewModel` seeds a set of default rules on first launch (premium prefixes, repeated digits, sequential digits, known spam, BD mobile patterns, prize-scam wording).

**Key files:**
- `app/src/main/java/com/callassistant/ui/screens/SpamRulesScreen.kt`
- `app/src/main/java/com/callassistant/data/repository/SpamRuleRepository.kt`
- `app/src/main/java/com/callassistant/service/CallScreeningServiceImpl.kt`

---

## 9. Call Handling

`CallAssistantInCallService` extends `InCallService` and tracks the current call. It launches `InCallActivity` for every call.

`InCallActivity` provides:

- **Incoming call:** caller name/number, pulsing avatar, slide-to-answer, decline, quick "Can't talk" SMS, speaker/record/note toggles.
- **Active call:** mute, DTMF keypad, speaker, record, call note, callback reminder (1 or 3 hours), end call.
- **Connecting call:** cancel, speaker, record, note.
- **Recording:** uses `CallRecorder` to record microphone audio to `CallRecordings/call_<number>_<timestamp>.m4a`.
- **Call notes:** saved via `CallNotesStore`.
- **Reminders:** scheduled via `AlarmManager` and delivered by `CallReminderReceiver` as a notification.

**Key files:**
- `app/src/main/java/com/callassistant/service/CallAssistantInCallService.kt`
- `app/src/main/java/com/callassistant/InCallActivity.kt`
- `app/src/main/java/com/callassistant/util/CallRecorder.kt`
- `app/src/main/java/com/callassistant/receiver/CallReminderReceiver.kt`
- `app/src/main/java/com/callassistant/receiver/CallActionReceiver.kt`

---

## 10. Recordings

`RecordingsScreen` lists all `.m4a` files saved by `CallRecorder`.

**Features:**
- Play/pause with `MediaPlayer`, including speaker routing reset.
- Seek slider for the active recording.
- Share via `FileProvider`.
- Delete a recording.

**Key files:**
- `app/src/main/java/com/callassistant/ui/screens/RecordingsScreen.kt`
- `app/src/main/java/com/callassistant/util/CallRecorder.kt`

---

## 11. Custom Keyboard

The app includes a custom IME, `CustomKeyboardService`, with a standard QWERTY-ish layout.

`KeyboardSettingsDialog` (opened from the top-bar settings icon) lets the user pick a background image from the gallery. The URI is persisted in `keyboard_prefs` and loaded as the keyboard background.

**Key files:**
- `app/src/main/java/com/callassistant/keyboard/CustomKeyboardService.kt`
- `app/src/main/java/com/callassistant/ui/components/KeyboardSettingsDialog.kt`

---

## 12. Data Sync & Deleted-Entry Tracking

`MainViewModel` exposes `StateFlow`s for contacts, call logs, SMS, spam rules, and blocked numbers. Syncers read from Android content providers and write to Room.

`DeletedEntriesStore` keeps local keys for user-deleted contacts, call logs, and SMS so they are not re-imported on the next sync.

**Key files:**
- `app/src/main/java/com/callassistant/ui/MainViewModel.kt`
- `app/src/main/java/com/callassistant/util/DeletedEntriesStore.kt`
- `app/src/main/java/com/callassistant/data/sync/*.kt`

---

## 13. Key Source Map

| Feature area | Primary files |
|---|---|
| Entry point / roles | `MainActivity.kt` |
| Navigation / scaffold | `ui/MainApp.kt` |
| Business logic / state | `ui/MainViewModel.kt` |
| Contacts UI | `ui/screens/ContactsScreen.kt` |
| Call log UI | `ui/screens/CallLogScreen.kt` |
| Dial pad / T9 | `ui/screens/DialPadScreen.kt` |
| SMS threads | `ui/screens/MessagesScreen.kt`, `MessageThreadScreen.kt` |
| Spam / block | `ui/screens/SpamRulesScreen.kt`, `data/repository/SpamRuleRepository.kt` |
| In-call screen | `InCallActivity.kt` |
| Call service | `service/CallAssistantInCallService.kt` |
| Call screening | `service/CallScreeningServiceImpl.kt` |
| Recording | `ui/screens/RecordingsScreen.kt`, `util/CallRecorder.kt` |
| SMS receiver | `receiver/SmsReceiver.kt` |
| Custom keyboard | `keyboard/CustomKeyboardService.kt` |
| Permissions | `permission/RequiredPermission.kt` |
| Database | `data/db/AppDatabase.kt`, `data/entity/*.kt` |
