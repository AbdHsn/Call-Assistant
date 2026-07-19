package com.callassistant.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.callassistant.data.entity.RuleType;
import com.callassistant.data.entity.SpamRule;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SpamRuleDao_Impl implements SpamRuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SpamRule> __insertionAdapterOfSpamRule;

  private final EntityDeletionOrUpdateAdapter<SpamRule> __deletionAdapterOfSpamRule;

  private final EntityDeletionOrUpdateAdapter<SpamRule> __updateAdapterOfSpamRule;

  public SpamRuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSpamRule = new EntityInsertionAdapter<SpamRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `spam_rules` (`id`,`pattern`,`type`,`label`,`isBlocking`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpamRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPattern());
        statement.bindString(3, __RuleType_enumToString(entity.getType()));
        statement.bindString(4, entity.getLabel());
        final int _tmp = entity.isBlocking() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSpamRule = new EntityDeletionOrUpdateAdapter<SpamRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `spam_rules` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpamRule entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSpamRule = new EntityDeletionOrUpdateAdapter<SpamRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `spam_rules` SET `id` = ?,`pattern` = ?,`type` = ?,`label` = ?,`isBlocking` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpamRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPattern());
        statement.bindString(3, __RuleType_enumToString(entity.getType()));
        statement.bindString(4, entity.getLabel());
        final int _tmp = entity.isBlocking() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final SpamRule rule, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSpamRule.insertAndReturnId(rule);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final SpamRule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSpamRule.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final SpamRule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSpamRule.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SpamRule>> getAll() {
    final String _sql = "SELECT * FROM spam_rules ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spam_rules"}, new Callable<List<SpamRule>>() {
      @Override
      @NonNull
      public List<SpamRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "pattern");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsBlocking = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocking");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SpamRule> _result = new ArrayList<SpamRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpamRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPattern;
            _tmpPattern = _cursor.getString(_cursorIndexOfPattern);
            final RuleType _tmpType;
            _tmpType = __RuleType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final boolean _tmpIsBlocking;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocking);
            _tmpIsBlocking = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SpamRule(_tmpId,_tmpPattern,_tmpType,_tmpLabel,_tmpIsBlocking,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getActiveBlockingRules(final Continuation<? super List<SpamRule>> $completion) {
    final String _sql = "SELECT * FROM spam_rules WHERE isBlocking = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SpamRule>>() {
      @Override
      @NonNull
      public List<SpamRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "pattern");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsBlocking = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocking");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SpamRule> _result = new ArrayList<SpamRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpamRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPattern;
            _tmpPattern = _cursor.getString(_cursorIndexOfPattern);
            final RuleType _tmpType;
            _tmpType = __RuleType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final boolean _tmpIsBlocking;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocking);
            _tmpIsBlocking = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SpamRule(_tmpId,_tmpPattern,_tmpType,_tmpLabel,_tmpIsBlocking,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __RuleType_enumToString(@NonNull final RuleType _value) {
    switch (_value) {
      case REGEX: return "REGEX";
      case EXACT: return "EXACT";
      case PREFIX: return "PREFIX";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private RuleType __RuleType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "REGEX": return RuleType.REGEX;
      case "EXACT": return RuleType.EXACT;
      case "PREFIX": return RuleType.PREFIX;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
