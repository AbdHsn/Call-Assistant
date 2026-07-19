package com.callassistant.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.callassistant.data.entity.CallLogEntry;
import com.callassistant.data.entity.CallType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
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
public final class CallLogDao_Impl implements CallLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallLogEntry> __insertionAdapterOfCallLogEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfMarkBlocked;

  public CallLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallLogEntry = new EntityInsertionAdapter<CallLogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `call_logs` (`id`,`number`,`name`,`type`,`timestamp`,`blocked`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallLogEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNumber());
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getName());
        }
        statement.bindString(4, __CallType_enumToString(entity.getType()));
        statement.bindLong(5, entity.getTimestamp());
        final int _tmp = entity.getBlocked() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_logs";
        return _query;
      }
    };
    this.__preparedStmtOfMarkBlocked = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE call_logs SET blocked = 1 WHERE number = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<CallLogEntry> entries,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallLogEntry.insert(entries);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markBlocked(final String number, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkBlocked.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, number);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkBlocked.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallLogEntry>> getAll() {
    final String _sql = "SELECT * FROM call_logs ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_logs"}, new Callable<List<CallLogEntry>>() {
      @Override
      @NonNull
      public List<CallLogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "number");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "blocked");
          final List<CallLogEntry> _result = new ArrayList<CallLogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNumber;
            _tmpNumber = _cursor.getString(_cursorIndexOfNumber);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final CallType _tmpType;
            _tmpType = __CallType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBlocked);
            _tmpBlocked = _tmp != 0;
            _item = new CallLogEntry(_tmpId,_tmpNumber,_tmpName,_tmpType,_tmpTimestamp,_tmpBlocked);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __CallType_enumToString(@NonNull final CallType _value) {
    switch (_value) {
      case INCOMING: return "INCOMING";
      case OUTGOING: return "OUTGOING";
      case MISSED: return "MISSED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private CallType __CallType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "INCOMING": return CallType.INCOMING;
      case "OUTGOING": return CallType.OUTGOING;
      case "MISSED": return CallType.MISSED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
