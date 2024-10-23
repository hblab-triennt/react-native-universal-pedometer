package com.t2tx.BMDPedometer;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import android.util.Log;

/**
 * 歩数関連 react native module (主にdebug用、正式機能は BMDPedometerを介すべき)
 */
public class StepModule extends ReactContextBaseJavaModule {
  private static final String TAG = "StepModule";

  private static ReactApplicationContext reactContext;
  private StepDatabase stepDatabase;
  private StepDao stepDao;

  StepModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;

    stepDatabase = StepDatabase.getInstance(reactContext);
    stepDao = stepDatabase.stepDao();
  }

  @Override
  public String getName() {
    return "Step";
  }

  @ReactMethod
  public void addStep(double date, int step, Promise promise) {
    stepDao.insertStep(new StepLog((long)date, step, -1));

    promise.resolve(null);
  }

  @ReactMethod
  public void queryLatestSteps(double date, Promise promise) {
    StepLog stepLog = stepDao.getLatestStep((long) date);

    if (stepLog != null) {
      WritableMap map = Arguments.createMap();
      map.putDouble("date", (double) stepLog.getTimeStamp());
      map.putInt("step", stepLog.getStep());

      Log.i(TAG, "queryLatestSteps: " + stepLog.getTimeStamp() + "/" + stepLog.getStep());
      promise.resolve(map);
    }
    promise.resolve(null);
  }

  @ReactMethod
  public void querySteps(double startDate, double endDate, Promise promise) {

  }

  @ReactMethod
  public void getLastClearLog(Promise promise) {
    ClearLog clearLog = stepDao.getClearLog(0);

    if(clearLog != null) {
      WritableMap map = Arguments.createMap();
      map.putDouble("date", (double)clearLog.getTimeStamp());
      map.putInt("id", clearLog.getId());

      promise.resolve(map);
    }
    promise.resolve(null);
  }

  @ReactMethod
  public void initClearLog(double date, Promise promise) {
    stepDao.insertClearLog(new ClearLog(0, (long)date));

    promise.resolve(null);
  }

  @ReactMethod
  public void clearAll(Promise promise) {
    stepDao.clearStepLog();
    stepDao.clearClearLog();

    promise.resolve(null);
  }
}
