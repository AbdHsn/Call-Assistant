# Call Assistant — Architecture

This app follows a **layered, feature-oriented** structure suitable for a single-module Compose dialer/SMS app.

## Layers

```
┌─────────────────────────────────────────────────────────────┐
│  UI (Compose)                                               │
│  screens/, components/, navigation/, feature ViewModels     │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  ViewModels (@HiltViewModel)                                 │
│  Expose StateFlow uiState; delegate work to repositories      │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  Data — repositories/                                       │
│  Room DAOs, ContentProvider syncers, SharedPreferences      │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  Android system (Telecom, Telephony, ContactsContract)      │
└─────────────────────────────────────────────────────────────┘
```

## Package map

| Package | Responsibility |
|---------|----------------|
| `ui/navigation` | Typed routes (`AppRoute`) — no magic strings |
| `ui/phonebook` | `PhoneBookViewModel` — contacts, call log, dial pad |
| `ui/notes` | `NotesViewModel` — call notes list |
| `ui/recordings` | `RecordingsViewModel` — recording file list |
| `ui/screens` | Compose screens (presentation only) |
| `ui/components` | Reusable UI (dialogs, guards, maps) |
| `data/repository` | Single source of truth per feature area |
| `data/db` | Room entities & DAOs |
| `data/sync` | ContentProvider → entity mappers |
| `di` | Hilt modules & entry points for receivers |
| `incall`, `service`, `sms`, `receiver` | Platform entry points |
| `util` | Cross-cutting helpers (phone normalize, photos, recorder) |

## ViewModels

| ViewModel | Scope | Repositories |
|-----------|-------|--------------|
| `MainViewModel` | App shell, theme, navigation | `SettingsRepository` |
| `PhoneBookViewModel` | Contacts, Call Log, Dial tabs | Contact, CallLog, SpamRule |
| `MessagesViewModel` | SMS threads | Sms, Contact |
| `SpamRulesViewModel` | Rules & blocked numbers | SpamRule |
| `NotesViewModel` | Call notes | Notes, Contact |
| `RecordingsViewModel` | `.m4a` files | (CallRecorder util) |

ViewModels are **scoped per destination** in `MainApp` (created only when a tab/screen is shown).

## Repositories

All repositories use **interface + `@Binds` implementation** in `RepositoryModule`:

- `ContactRepository` — sync/save/delete contacts
- `CallLogRepository` — sync/delete/mark blocked
- `SmsRepository` — sync/save incoming/outgoing/delete
- `SpamRuleRepository` — rules, blocking, default seed
- `SettingsRepository` — theme & app preferences
- `NotesRepository` — call notes persistence

**Rule:** UI and services talk to repositories, not DAOs directly.

## Navigation

- Bottom tabs: `AppRoute.CallLog`, `Contacts`, `DialPad`, `Messages`
- Settings menu: Notes, Spam, Recordings, Recorder settings, Setup
- Default tab: **Dial**
- Routes are `AppRoute` sealed types in `ui/navigation/AppRoute.kt`

## Dependency injection

- `@HiltAndroidApp` on `CallAssistantApplication`
- `@AndroidEntryPoint` on activities/services that need injection
- `@HiltViewModel` on ViewModels
- `AppEntryPoint` for `BroadcastReceiver` classes (`SmsReceiver`, `CallActionReceiver`)

## Adding a feature

1. Add repository method (or new repository) if data access is needed
2. Add/update ViewModel with `StateFlow` uiState
3. Add Composable screen under `ui/screens/`
4. Register route in `AppRoute` and wire in `MainApp`
5. Bind new repository in `RepositoryModule` if applicable

## Conventions

- **Phone matching:** use `PhoneNumberNormalizer.matches()` everywhere
- **Tel URIs:** use `telCallUri()` for USSD codes (`*566#`)
- **Blocking:** `SpamRuleRepository.isNumberBlocked()` is the single source of truth
- **Local-only contact fields** (photo, address, map): saved contacts are marked `ContactSource.LOCAL` so sync does not wipe them
