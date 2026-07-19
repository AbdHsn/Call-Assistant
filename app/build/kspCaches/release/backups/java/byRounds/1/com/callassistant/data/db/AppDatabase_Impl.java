package com.callassistant.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ContactDao _contactDao;

  private volatile CallLogDao _callLogDao;

  private volatile SmsDao _smsDao;

  private volatile SpamRuleDao _spamRuleDao;

  private volatile BlockedNumberDao _blockedNumberDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phoneNumber` TEXT NOT NULL, `source` TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_phoneNumber` ON `contacts` (`phoneNumber`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `call_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, `name` TEXT, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `blocked` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_logs_number` ON `call_logs` (`number`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_logs_timestamp` ON `call_logs` (`timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sms_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, `name` TEXT, `body` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `direction` TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_messages_number` ON `sms_messages` (`number`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_messages_timestamp` ON `sms_messages` (`timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `spam_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pattern` TEXT NOT NULL, `type` TEXT NOT NULL, `label` TEXT NOT NULL, `isBlocking` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `blocked_numbers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` TEXT NOT NULL, `reason` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blocked_numbers_number` ON `blocked_numbers` (`number`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '01b239ab0e920c4cd08d1716d280fe0a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `contacts`");
        db.execSQL("DROP TABLE IF EXISTS `call_logs`");
        db.execSQL("DROP TABLE IF EXISTS `sms_messages`");
        db.execSQL("DROP TABLE IF EXISTS `spam_rules`");
        db.execSQL("DROP TABLE IF EXISTS `blocked_numbers`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsContacts = new HashMap<String, TableInfo.Column>(4);
        _columnsContacts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContacts = new HashSet<TableInfo.Index>(1);
        _indicesContacts.add(new TableInfo.Index("index_contacts_phoneNumber", true, Arrays.asList("phoneNumber"), Arrays.asList("ASC")));
        final TableInfo _infoContacts = new TableInfo("contacts", _columnsContacts, _foreignKeysContacts, _indicesContacts);
        final TableInfo _existingContacts = TableInfo.read(db, "contacts");
        if (!_infoContacts.equals(_existingContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "contacts(com.callassistant.data.entity.Contact).\n"
                  + " Expected:\n" + _infoContacts + "\n"
                  + " Found:\n" + _existingContacts);
        }
        final HashMap<String, TableInfo.Column> _columnsCallLogs = new HashMap<String, TableInfo.Column>(6);
        _columnsCallLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLogs.put("number", new TableInfo.Column("number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLogs.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLogs.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLogs.put("blocked", new TableInfo.Column("blocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCallLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCallLogs = new HashSet<TableInfo.Index>(2);
        _indicesCallLogs.add(new TableInfo.Index("index_call_logs_number", false, Arrays.asList("number"), Arrays.asList("ASC")));
        _indicesCallLogs.add(new TableInfo.Index("index_call_logs_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoCallLogs = new TableInfo("call_logs", _columnsCallLogs, _foreignKeysCallLogs, _indicesCallLogs);
        final TableInfo _existingCallLogs = TableInfo.read(db, "call_logs");
        if (!_infoCallLogs.equals(_existingCallLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "call_logs(com.callassistant.data.entity.CallLogEntry).\n"
                  + " Expected:\n" + _infoCallLogs + "\n"
                  + " Found:\n" + _existingCallLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsSmsMessages = new HashMap<String, TableInfo.Column>(6);
        _columnsSmsMessages.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsMessages.put("number", new TableInfo.Column("number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsMessages.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsMessages.put("body", new TableInfo.Column("body", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsMessages.put("direction", new TableInfo.Column("direction", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSmsMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSmsMessages = new HashSet<TableInfo.Index>(2);
        _indicesSmsMessages.add(new TableInfo.Index("index_sms_messages_number", false, Arrays.asList("number"), Arrays.asList("ASC")));
        _indicesSmsMessages.add(new TableInfo.Index("index_sms_messages_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoSmsMessages = new TableInfo("sms_messages", _columnsSmsMessages, _foreignKeysSmsMessages, _indicesSmsMessages);
        final TableInfo _existingSmsMessages = TableInfo.read(db, "sms_messages");
        if (!_infoSmsMessages.equals(_existingSmsMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "sms_messages(com.callassistant.data.entity.SmsMessage).\n"
                  + " Expected:\n" + _infoSmsMessages + "\n"
                  + " Found:\n" + _existingSmsMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsSpamRules = new HashMap<String, TableInfo.Column>(6);
        _columnsSpamRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpamRules.put("pattern", new TableInfo.Column("pattern", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpamRules.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpamRules.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpamRules.put("isBlocking", new TableInfo.Column("isBlocking", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpamRules.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSpamRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSpamRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSpamRules = new TableInfo("spam_rules", _columnsSpamRules, _foreignKeysSpamRules, _indicesSpamRules);
        final TableInfo _existingSpamRules = TableInfo.read(db, "spam_rules");
        if (!_infoSpamRules.equals(_existingSpamRules)) {
          return new RoomOpenHelper.ValidationResult(false, "spam_rules(com.callassistant.data.entity.SpamRule).\n"
                  + " Expected:\n" + _infoSpamRules + "\n"
                  + " Found:\n" + _existingSpamRules);
        }
        final HashMap<String, TableInfo.Column> _columnsBlockedNumbers = new HashMap<String, TableInfo.Column>(4);
        _columnsBlockedNumbers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedNumbers.put("number", new TableInfo.Column("number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedNumbers.put("reason", new TableInfo.Column("reason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedNumbers.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBlockedNumbers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBlockedNumbers = new HashSet<TableInfo.Index>(1);
        _indicesBlockedNumbers.add(new TableInfo.Index("index_blocked_numbers_number", true, Arrays.asList("number"), Arrays.asList("ASC")));
        final TableInfo _infoBlockedNumbers = new TableInfo("blocked_numbers", _columnsBlockedNumbers, _foreignKeysBlockedNumbers, _indicesBlockedNumbers);
        final TableInfo _existingBlockedNumbers = TableInfo.read(db, "blocked_numbers");
        if (!_infoBlockedNumbers.equals(_existingBlockedNumbers)) {
          return new RoomOpenHelper.ValidationResult(false, "blocked_numbers(com.callassistant.data.entity.BlockedNumber).\n"
                  + " Expected:\n" + _infoBlockedNumbers + "\n"
                  + " Found:\n" + _existingBlockedNumbers);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "01b239ab0e920c4cd08d1716d280fe0a", "5145bfe95e2ec7745aa55ceed643ff3f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "contacts","call_logs","sms_messages","spam_rules","blocked_numbers");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `contacts`");
      _db.execSQL("DELETE FROM `call_logs`");
      _db.execSQL("DELETE FROM `sms_messages`");
      _db.execSQL("DELETE FROM `spam_rules`");
      _db.execSQL("DELETE FROM `blocked_numbers`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ContactDao.class, ContactDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CallLogDao.class, CallLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SmsDao.class, SmsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SpamRuleDao.class, SpamRuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BlockedNumberDao.class, BlockedNumberDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ContactDao contactDao() {
    if (_contactDao != null) {
      return _contactDao;
    } else {
      synchronized(this) {
        if(_contactDao == null) {
          _contactDao = new ContactDao_Impl(this);
        }
        return _contactDao;
      }
    }
  }

  @Override
  public CallLogDao callLogDao() {
    if (_callLogDao != null) {
      return _callLogDao;
    } else {
      synchronized(this) {
        if(_callLogDao == null) {
          _callLogDao = new CallLogDao_Impl(this);
        }
        return _callLogDao;
      }
    }
  }

  @Override
  public SmsDao smsDao() {
    if (_smsDao != null) {
      return _smsDao;
    } else {
      synchronized(this) {
        if(_smsDao == null) {
          _smsDao = new SmsDao_Impl(this);
        }
        return _smsDao;
      }
    }
  }

  @Override
  public SpamRuleDao spamRuleDao() {
    if (_spamRuleDao != null) {
      return _spamRuleDao;
    } else {
      synchronized(this) {
        if(_spamRuleDao == null) {
          _spamRuleDao = new SpamRuleDao_Impl(this);
        }
        return _spamRuleDao;
      }
    }
  }

  @Override
  public BlockedNumberDao blockedNumberDao() {
    if (_blockedNumberDao != null) {
      return _blockedNumberDao;
    } else {
      synchronized(this) {
        if(_blockedNumberDao == null) {
          _blockedNumberDao = new BlockedNumberDao_Impl(this);
        }
        return _blockedNumberDao;
      }
    }
  }
}
