package com.t2tx.BMDPedometer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 歩数記録のクリアログ（基本一行のみ）
 */
@Entity
public class ClearLog {
  @PrimaryKey
  @ColumnInfo(name="id")
  private final int mId;

  @ColumnInfo(name = "timestamp")
  private final long mTimeStamp;

  /**
   * 構築関数
   */
  public ClearLog(int id, long timeStamp) {
    this.mId = id;
    this.mTimeStamp = timeStamp;
  }

  /**
   * IDの取得
   */
  public int getId() {
    return mId;
  }

  /**
   * タイムスタンプの取得（unix ms）
   */
  public long getTimeStamp() {
    return mTimeStamp;
  }
}