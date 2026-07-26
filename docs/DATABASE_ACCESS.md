# Accessing the Call Assistant Database

The app uses a **Room** database defined in
`app/src/main/java/com/callassistant/data/db/AppDatabase.kt`.

- **Name:** `call_assistant_db`
- **Location on device:** `/data/data/com.callassistant/databases/call_assistant_db`
  (plus `call_assistant_db-wal` / `call_assistant_db-shm` journal files)
- **Tables (entities):** `Contact`, `CallLogEntry`, `SmsMessage`, `SpamRule`, `BlockedNumber`

This path is inside the app's private storage, so it is **not** visible in a normal
file manager and requires one of the methods below.

---

## Option 1: Android Studio Database Inspector (easiest)

Works on a debug build without root.

1. Run the app on a device/emulator from Android Studio (Run ▶).
2. Open **View → Tool Windows → App Inspection**.
3. Select the **Database Inspector** tab.
4. Pick the `com.callassistant` process, then `call_assistant_db`.
5. Browse tables, run live SQL queries, and even edit rows while the app is running.

---

## Option 2: adb (debuggable build, no root required)

Since the app is a debug build, `run-as` can access its private data directory.

```bash
# List the database files
adb shell run-as com.callassistant ls -l /data/data/com.callassistant/databases/

# Copy the database (and WAL files) to the device's shared storage
adb shell run-as com.callassistant cp /data/data/com.callassistant/databases/call_assistant_db /sdcard/call_assistant_db
adb shell run-as com.callassistant cp /data/data/com.callassistant/databases/call_assistant_db-wal /sdcard/call_assistant_db-wal
adb shell run-as com.callassistant cp /data/data/com.callassistant/databases/call_assistant_db-shm /sdcard/call_assistant_db-shm

# Pull the files to your machine
adb pull /sdcard/call_assistant_db .
adb pull /sdcard/call_assistant_db-wal .
adb pull /sdcard/call_assistant_db-shm .

# Clean up the copies left on the device (optional)
adb shell rm /sdcard/call_assistant_db /sdcard/call_assistant_db-wal /sdcard/call_assistant_db-shm
```

> If `run-as` fails with "package not debuggable", make sure you built/installed the
> **debug** variant (`./gradlew installDebug`), not release.

On a **rooted** device/emulator, you can skip `run-as` and use `adb root` + `adb shell` directly,
or just `adb pull /data/data/com.callassistant/databases/call_assistant_db .`

---

## Option 3: Inspect with a third-party SQLite browser

Once you have the `.db` file locally (from Option 2), open it with any SQLite GUI, e.g.:

- **DB Browser for SQLite** ([sqlitebrowser.org](https://sqlitebrowser.org)) — free, cross-platform GUI.
  ```bash
  sudo apt install sqlitebrowser   # Debian/Ubuntu
  sqlitebrowser call_assistant_db
  ```
- **VS Code extension**: "SQLite" or "SQLite Viewer" — open the pulled `.db` file directly in the editor.
- **sqlite3 CLI** (already on most Linux systems):
  ```bash
  sqlite3 call_assistant_db
  sqlite> .tables
  sqlite> SELECT * FROM contacts LIMIT 20;
  ```

> Because Room uses WAL mode, make sure the `-wal` and `-shm` files sit next to the `.db`
> file in the same folder before opening it, otherwise recent uncommitted writes may be missing.
> For a fully consistent snapshot, close the app before pulling, or checkpoint the WAL first:
> `adb shell run-as com.callassistant sqlite3 /data/data/com.callassistant/databases/call_assistant_db "PRAGMA wal_checkpoint(FULL);"`

---

## Notes

- `AppDatabase.getDatabase()` uses `fallbackToDestructiveMigration()`, so schema changes between
  app versions can wipe existing data — keep this in mind when debugging "missing data" issues.
- `exportSchema = false` is set in the `@Database` annotation, so there's no bundled JSON schema
  history to diff against; rely on the entity classes under
  `app/src/main/java/com/callassistant/data/entity/` for the current schema.
