# Call Assistant — Same-Day MVP Plan

Scope: Android app running on the **user's own device**, with explicit runtime permissions.
Excluded (not feasible/legit as background features): reading another account's WhatsApp, hidden call recording.

## 1. Stack
- **Platform:** Android (Kotlin)
- **Database:** SQLite via **Room** (Android's official ORM over SQLite)
  - Why: local-first, zero setup, fast lookups, works fully offline, easy migrations later if you add a cloud sync layer.
  - Alternative if you want cross-device sync later: keep Room as local cache + Firebase Firestore/Supabase for sync. Not needed for today's MVP.

## 2. Data Model (Room entities)
```
Contact(id, name, phoneNumber, source[local/imported])
CallLog(id, number, name?, type[incoming/outgoing/missed], timestamp, blocked:boolean)
SmsMessage(id, number, name?, body, timestamp, direction[in/out])
SpamRule(id, pattern, type[regex/exact/prefix], label, isBlocking:boolean, createdAt)
BlockedNumber(id, number, reason, timestamp)
```

## 3. Feature Checklist (ranked by build speed)

| # | Feature | API / Approach | Effort |
|---|---------|----------------|--------|
| 1 | Runtime permission flow | `ActivityResultContracts.RequestMultiplePermissions()` — request CONTACTS, CALL_LOG, SMS on demand, show rationale dialog first | Low |
| 2 | Contact access | `ContentResolver` + `ContactsContract.Contacts` — read into Room table | Low |
| 3 | Call blocking | `CallScreeningService` (Android 10+) — return `CallResponse.Builder().setDisallowCall(true)` when number matches SpamRule table | Medium |
| 4 | Call log view | `CallLog.Calls` content provider, synced into Room | Low |
| 5 | SMS view/send | Set app as default SMS handler → `Telephony.Sms` provider for read, `SmsManager.sendTextMessage()` for send | Medium |
| 6 | Spam detection engine | Local rule table: for each incoming number/SMS body, run through active SpamRule list (regex `Pattern.matcher()`, or prefix/exact match); user can add/edit/delete rules in a settings UI | Medium |
| 7 | Custom keyboard (IME) | `InputMethodService` subclass + custom layout XML; support user-chosen background image via `ImageView` behind keys, stored in SharedPreferences/Room | Medium-High |
| 8 | Dial pad customization | Custom in-app dialer UI (Compose) as an alternative to system dialer — themeable keys, custom digit font/layout, optional background image, haptic press feedback; register as a **default dialer app** (`RoleManager.createRequestRoleIntent(ROLE_DIALER)`) to fully replace the system dial pad | Medium |
| 9 | In-call screen customization (Phase 2) | Implement `InCallService` + `Call.Callback` to show a fully custom in-call UI — caller name/photo, custom mute/speaker/hold/end buttons, theme-matched design, animated call-connecting state. **Requires default dialer role** — Android won't let a non-default app show a custom in-call screen | High |
| 10 | Permission-gated feature gating | Each feature checks its own permission before running; if missing, prompt in-context ("Enable X to use Y") | Low |

## 4. Today's Build Order (fastest path to a working demo)
1. Project scaffold + Room DB setup (entities above) — **30–45 min**
2. Permission request flow (contacts, call log, SMS) — **30 min**
3. Contact sync into Room + simple list UI — **45 min**
4. SpamRule CRUD screen (add regex/prefix rules, toggle block) — **45 min**
5. `CallScreeningService` wired to SpamRule matching — **1 hr**
6. Call log sync + "blocked" badge in UI — **30 min**
7. Default SMS handler setup + read/send + spam-check on incoming SMS — **1.5 hr**
8. Basic custom keyboard IME with placeholder + image background support — **1.5–2 hr** (most time-consuming piece; can stub as "coming next" if time-constrained)
9. Custom dial pad screen (Compose UI, theme-aware, default-dialer role request) — **1–1.5 hr**

**Realistic same-day scope:** items 1–6 fully working, SMS as stretch goal, keyboard and dial pad as basic proof-of-concept (not polished).

### Phase 2 (not today — separate build session)
10. `InCallService` implementation + custom in-call screen (caller info, custom controls, theming) — **4–6 hr**, including:
    - Handling call states (ringing, active, hold, disconnected) via `Call.Callback`
    - Audio routing (speaker/earpiece/Bluetooth) via `CallAudioState`
    - Emergency call compliance checks (required by Google Play policy for dialer-role apps)
    - Testing across real call scenarios (can't be fully tested in an emulator — needs a real device with a SIM)

This is meaningfully heavier than the dial pad since it's tied into live telephony state, not just a static UI — worth treating as its own milestone rather than squeezing into today's build.

## 5. Spam Rule Examples (seed data)
```
regex: ^\+?880?1[3-9]\d{8}$        -> label: "Unverified BD mobile pattern"
prefix: +1900                       -> label: "Premium/scam prefix"
exact: 000000000                    -> label: "Known spam"
```
Rules stored in SpamRule table; matching engine just iterates and applies `isBlocking` flag.

## 6. UI/UX, Performance & Theming

### Performance
- Use **Jetpack Compose** instead of legacy XML views — better recomposition performance, easier to keep animations smooth at 60fps.
- All DB reads/writes on `Dispatchers.IO` via Room's suspend/Flow APIs — never touch DB on main thread.
- Use `Flow`/`LiveData` from Room so lists (contacts, call log, spam rules) auto-update without manual refresh calls.
- Lazy-load call log/SMS lists with `LazyColumn` + paging (`Paging 3` library) if lists grow large — avoids jank on big histories.
- Debounce spam-rule regex evaluation so typing a new rule doesn't re-run matches on every keystroke.

### Responsive Design
- Build layouts with Compose `BoxWithConstraints` / adaptive breakpoints so the UI reflows properly across phone sizes and tablets/foldables.
- Support both portrait and landscape without losing state (use `rememberSaveable` for UI state).
- Scale touch targets and text with `sp`/`dp` (not fixed px) for accessibility across screen densities.

### Dark & Light Theme
- Implement via Compose `MaterialTheme` with two `ColorScheme`s (light/dark) and support **"Follow system"** as default, with manual override toggle.
- Use Material 3 dynamic color (Android 12+) so the theme can optionally pull accent colors from the user's wallpaper, falling back to a fixed brand palette on older versions.
- Keep a single source-of-truth theme state (DataStore) so the choice persists across app restarts and syncs to the keyboard IME's background/theme too.

### Animation & Icons
- Use **Lottie** (via `lottie-compose`) for polished vector animations — e.g., a subtle pulse/ripple when a spam call is blocked, a check animation when a rule is saved.
- Micro-interactions with Compose's built-in `animateColorAsState`, `AnimatedVisibility`, and `Crossfade` for theme switches, list item add/remove, and tab transitions — cheap and GPU-friendly.
- Icon set: Material Symbols (outlined for light theme, filled/tonal for dark) for consistency; animate icon state changes (e.g., bell → bell-slash on block) with `AnimatedContent`.
- Keep animation durations short (150–250ms) and respect the system's "reduce motion" accessibility setting.

### Interactivity
- Swipe-to-block / swipe-to-delete on call log and spam-rule list items (`SwipeToDismissBox` in Compose).
- Haptic feedback on block/allow actions for tactile confirmation.
- Live preview of regex spam rules as the user types (highlight matching sample numbers in real time).
- Pull-to-refresh on synced lists (contacts/call log).

## 7. Known Platform Constraints
- Call blocking requires user to set your app as a **call-screening app** in system settings (Android won't allow silent background blocking without this).
- Being default SMS app hands you SMS read/send, but replaces the user's regular SMS app — worth a clear onboarding explanation.
- Custom keyboards need the user to manually enable them in **Settings > System > Languages & input** — cannot be auto-enabled.
- A custom dial pad only fully replaces the system one if the user grants **default dialer** role — otherwise it can exist as an in-app screen but won't intercept the phone's native dial UI.
- A custom in-call screen is **only possible if the app holds the default dialer role** — there's no way to overlay or replace the in-call UI otherwise. Google Play also requires dialer-role apps to correctly handle emergency calls, so this needs real compliance testing before release, not just a demo build.
