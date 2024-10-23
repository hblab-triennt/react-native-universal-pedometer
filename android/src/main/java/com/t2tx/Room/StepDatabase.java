package com.t2tx.BMDPedometer;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 歩数ローカルDB（singleton）
 * TODO: allowMainThreadQueriesの除去
 */
@Database(entities={StepLog.class, ClearLog.class}, version = 2)
public abstract class StepDatabase extends RoomDatabase {
  private static StepDatabase INSTANCE;
  public abstract StepDao stepDao();

  private static final Object sLock = new Object();

  private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE StepLog ADD COLUMN timeAfterReboot INTEGER NOT NULL DEFAULT -1");
    }
  };

  public static StepDatabase getInstance(Context context) {
    synchronized (sLock) {
      if (INSTANCE == null) {
        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
          StepDatabase.class, "react-native-universal-pedometer-steps.db")
          .addMigrations(MIGRATION_1_2)
          .allowMainThreadQueries()
          .build();
      }
      return INSTANCE;
    }
  }
}
