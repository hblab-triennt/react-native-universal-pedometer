package com.t2tx.BMDPedometer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * 歩数DBアクセスI/F
 */
@Dao
public interface StepDao {
  /**
   * クリアログの取得
   */
  @Query("SELECT * FROM ClearLog WHERE id = :id LIMIT 1")
  ClearLog getClearLog(int id);

  /**
   * 指定タイミングより古い歩数記録を削除する
   */
  @Query("DELETE FROM StepLog WHERE timestamp < :expire")
  void removeExpiredSteps(long expire);

  /**
   * 指定タイミング以前最も新鮮な歩数記録を取得する
   */
  @Query("SELECT * FROM StepLog WHERE timestamp <= :limit ORDER BY timestamp DESC LIMIT 1")
  StepLog getLatestStep(long limit);

  /**
   * 歩数記録の追加
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertStep(StepLog stepLog);

  /**
   * クリア記録の追加
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertClearLog(ClearLog clearLog);

  /**
   * 歩数記録のクリア
   */
  @Query("DELETE FROM StepLog")
  void clearStepLog();

  /**
   * クリア記録の全削除
   */
  @Query("DELETE FROM ClearLog")
  void clearClearLog();
}
