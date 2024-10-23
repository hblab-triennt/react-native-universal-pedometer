package com.t2tx.BMDPedometer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 歩数記録
 */
@Entity
public class StepLog {
  @PrimaryKey
  @ColumnInfo(name="timestamp")
  private final long mTimeStamp;

  @ColumnInfo(name = "step")
  private final int mStep;

  // DB version 2で追加
  @ColumnInfo(name="timeAfterReboot", defaultValue = "-1")
  private final long mTimeAfterReboot;

  /**
   * 構築関数
   */
  public StepLog(long timeStamp, int step, long timeAfterReboot) {
    this.mTimeStamp = timeStamp;
    this.mStep = step;
    this.mTimeAfterReboot = timeAfterReboot;
  }

  /**
   * 記録タイムスタンプの取得（unix ms）
   */
  public long getTimeStamp() {
    return mTimeStamp;
  }

  /**
   * 歩数の取得
   */
  public int getStep() {
    return mStep;
  }

  /**
   * 端末再起動後の経過時間の取得（unix ms）
  */
  public long getTimeAfterReboot() {
    return mTimeAfterReboot;
  }
}
