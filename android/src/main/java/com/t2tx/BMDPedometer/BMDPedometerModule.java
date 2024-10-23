package com.t2tx.BMDPedometer;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.Manifest;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.util.Log;

import androidx.annotation.Nullable;

public class BMDPedometerModule extends ReactContextBaseJavaModule implements StepListener, SensorEventListener, LifecycleEventListener {
  private static final String TAG = "BMDPedometerModule";

  ReactApplicationContext reactContext;

  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;
  public static int ERROR_NO_SENSOR_FOUND = 4;
  public static float STEP_IN_METERS = 0.762f;
  public static long LIMIT_OF_CLEAR_MS = 12 * 3600 * 1000;
  public static long LIMIT_OF_LOG_MS = 7 * 24 * 3600 * 1000;
  public static int CLEAR_LOG_ID = 0;

  private static final int STEP_PERSIST_DELAY_MS = 10000;
  private static final int STEP_START_HOUR = 0;
  private long lastPersistTime = 0;

  private int status;     // status of listener
  private int numSteps; // number of the steps
  private int startNumSteps; //first value, to be substracted in step counter sensor type
  private int rebootNumSteps; //the steps fetched from local db before latest reboot
  private int tooOldError;  // timeAfterReboot continuous error count
  private long startAt; //time stamp of when the measurement starts
  private long clearAt; //time stamp of when clear the old logs
  private StepDatabase stepDatabase; //local databse
  private StepDao stepDao; // local database dao

  private SensorManager sensorManager; // Sensor manager
  private Sensor mSensor;             // Pedometer sensor returned by sensor manager

  // for TYPE_ACCELEROMETER
  private int startAclSteps; // first value, to be added in TYPE_ACCELEROMETER type
  private StepDetector stepDetector;

  public BMDPedometerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.reactContext.addLifecycleEventListener(this);

    this.rebootNumSteps = -1;
    this.clearAt = 0;
    this.stepDatabase = StepDatabase.getInstance(reactContext);
    this.stepDao = stepDatabase.stepDao();
    this.tooOldError = 0;

    ClearLog clearLog = this.stepDao.getClearLog(CLEAR_LOG_ID);
    if (clearLog != null) {
      this.clearAt = clearLog.getTimeStamp();
    }

    this.setStatus(BMDPedometerModule.STOPPED);

    this.initSensorManager();

    this.startPedometerUpdatesFromDate(this.startOfToday());

    // init accr listener
    this.stepDetector = new StepDetector();
    this.stepDetector.registerListener(this);

    Log.d(TAG, "ctor done");
  }

  @Override
  public String getName() {
    return "BMDPedometer";
  }

  @ReactMethod
  public void isStepCountingAvailable(Callback callback) {
    if (this.sensorManager == null) {
      this.initSensorManager();
      if (this.sensorManager == null) {
        return;
      }
    }

    if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
      Log.d(TAG, "permission denied");
    }

    Sensor stepCounter = this.sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    Sensor accel = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (stepCounter != null || accel != null) {
      callback.invoke(null, true);
    } else {
      this.setStatus(BMDPedometerModule.ERROR_NO_SENSOR_FOUND);
      callback.invoke("Error: step counting is not available. BMDPedometerModule.ERROR_NO_SENSOR_FOUND", false);
    }

    //check is running
    if ((this.status != BMDPedometerModule.RUNNING) && (this.status != BMDPedometerModule.STARTING)) {
      this.startPedometerUpdatesFromDate(this.startOfToday());
    }
  }

  @ReactMethod
  public void iWantToDie(Callback callback) {
    float i = 3.0f / 0.0f;
  }

  @ReactMethod
  public void isDistanceAvailable(Callback callback) {
    callback.invoke(null, false);
  }

  @ReactMethod
  public void isFloorCountingAvailable(Callback callback) {
    callback.invoke(null, false);
  }

  @ReactMethod
  public void isPaceAvailable(Callback callback) {
    callback.invoke(null, false);
  }

  @ReactMethod
  public void isCadenceAvailable(Callback callback) {
    callback.invoke(null, false);
  }

  @ReactMethod
  public void startPedometerUpdatesFromDate(double date) {
    this.doStartAt((long) date);
    this.start();
  }

  @ReactMethod
  public void stopPedometerUpdates() {
    if (this.status == BMDPedometerModule.RUNNING) {
      this.stop();
    }
  }

  @ReactMethod
  public void queryPedometerDataBetweenDates(double startDate, double endDate, Callback callback) {
    try {
      int startSteps = 0;
      int steps = -1;
      //get record from local
      Log.i(TAG, "query: " + (long) startDate + "/" + (long) endDate + ',' + this.status);
      StepLog stepLog = stepDao.getLatestStep((long) startDate);
      if (stepLog != null) {
        startSteps = stepLog.getStep();
        stepLog = stepDao.getLatestStep((long) endDate);
        if (stepLog != null) {
          steps = stepLog.getStep() - startSteps;
        }
      }
      //return it
      WritableMap map = Arguments.createMap();
      map.putDouble("startDate", startDate);
      map.putDouble("endDate", endDate);
      map.putInt("numberOfSteps", steps);
      map.putDouble("distance", steps * BMDPedometerModule.STEP_IN_METERS);

      callback.invoke(null, map);
    } catch (Exception e) {
      callback.invoke(e.getMessage(), null);
    }
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @Override
  public void onHostResume() {
  }

  @Override
  public void onHostPause() {
  }

  @Override
  public void onHostDestroy() {
  }

  /**
   * Called when the accuracy of the sensor has changed.
   */
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    //nothing to do here
    return;
  }

  /**
   * Sensor listener event.
   *
   * @param event
   */
  @Override
  public void onSensorChanged(SensorEvent event) {
    // Only look at step counter or accelerometer events
    if (event.sensor.getType() != this.mSensor.getType()) {
      return;
    }

    // If not running, then just return
    if (this.status == BMDPedometerModule.STOPPED) {
      return;
    }

    if (this.mSensor.getType() == Sensor.TYPE_STEP_COUNTER) {
      this.onStepCounterSensorChanged(event);
    } else {
      this.onAccSensorChanged(event);
    }


    this.setStatus(BMDPedometerModule.RUNNING);
  }

  @Override
  public void step(long timeNs) {
    this.numSteps++;
    // // update local db
    saveSteps(this.numSteps + this.startNumSteps, -1);
    try {
      this.sendPedometerUpdateEvent(this.getStepsParamsMap());
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  // private
  private void initSensorManager() {
    if (this.sensorManager == null) {
      this.sensorManager = (SensorManager) this.reactContext.getSystemService(Context.SENSOR_SERVICE);
      if (this.sensorManager == null) {
        Log.e(TAG, "sensorManager is null");
      }
    }
  }

  private void onAccSensorChanged(SensorEvent event) {
    int stepAfterReboot = 0;
    long timeAfterReboot = System.currentTimeMillis() - this.startOfToday();

    try {
      // clear old log
      doExpire();

      // DB記録ない時のstartNumStepsの補正処理（時刻を先日に）
      if(this.startNumSteps < 0) {
        this.initDbRecoreds(timeAfterReboot, stepAfterReboot);
        this.startNumSteps = stepAfterReboot;
      }
      if (stepDetector != null) {
        stepDetector.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
      }
    } catch (Exception e) {
      Log.e(TAG, "ACC onSensorChanged error", e);
      e.printStackTrace();
    }
  }

  private void onStepCounterSensorChanged(SensorEvent event) {
    int stepAfterReboot = (int) event.values[0];
    long timeAfterReboot = (long) (event.timestamp / 1000000);

    try {
      // clear old log
      doExpire();

      // DB記録ない時のstartNumStepsの補正処理（時刻を先日に）
      if (this.startNumSteps < 0) {
        this.initDbRecoreds(timeAfterReboot, stepAfterReboot);
        this.startNumSteps = stepAfterReboot;
        this.rebootNumSteps = 0;
      }

      // 3回連続でtimeAfterRebootの値が古すぎると判定された場合、
      // 直前に記録されたtimeAfterRebootの方が異常値の可能性が高いため、
      // チェックをスキップする。（復帰処理）
      if (this.isTooOld(timeAfterReboot) && this.tooOldError < 3) {
        this.tooOldError++;
        return;
      }
      this.tooOldError = 0;

      // fetch steps before reboot
      if (this.rebootNumSteps < 0) {
        fetchRebootNumSteps((long) (timeAfterReboot));
      }

      Log.d(TAG, event.timestamp + "/" + event.values[0]);
      Log.d(TAG, "sensor startNumSteps: " + this.startNumSteps);
      Log.d(TAG, "sensor rebootNumSteps: " + this.rebootNumSteps);


      this.numSteps = stepAfterReboot + this.rebootNumSteps - this.startNumSteps;
      // update local db
      saveSteps(stepAfterReboot + this.rebootNumSteps, timeAfterReboot);


      this.sendPedometerUpdateEvent(this.getStepsParamsMap());
    } catch (Exception e) {
      Log.e(TAG, "CNT onSensorChanged error", e);
      e.printStackTrace();
    }
  }

  // timeAfterRebootに古すぎる値が入ってくることがあるのでチェック
  private boolean isTooOld(long timeAfterReboot) {
    long now = System.currentTimeMillis();
    if (now - lastPersistTime > STEP_PERSIST_DELAY_MS) {
      StepLog stepLog = stepDao.getLatestStep(now);
      if (stepLog != null) {
        long logTimeAfterReboot = stepLog.getTimeAfterReboot();
        if (logTimeAfterReboot >= 0) {
          long logTimeStamp = stepLog.getTimeStamp();
          long difTimeStamp = now - logTimeStamp;
          long difTimeAfterReboot = timeAfterReboot - logTimeAfterReboot;
          // 「現在時刻と、前回記録時時刻の差」と
          // 「現在の端末再起動後の経過時間と、前回記録時の端末再起動後の経過時の差」で
          // 30分以上も差がある場合は整合が取れていないとみなす
          return Math.abs(difTimeStamp - difTimeAfterReboot) > 1800000;
        }
      }
    }
    return false;
  }

  private long startOfToday() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    // 時がSTEP_START_HOURに満たない場合は、日付を1日前にずらす
    if (cal.get(Calendar.HOUR_OF_DAY) < STEP_START_HOUR)  {
      cal.add(Calendar.DAY_OF_MONTH, -1);
    }
    cal.set(Calendar.HOUR_OF_DAY, STEP_START_HOUR); //set hours to STEP_START_HOUR
    cal.set(Calendar.MINUTE, 0); // set minutes to zero
    cal.set(Calendar.SECOND, 0); //set seconds to zero
    return cal.getTimeInMillis();
  }

  private void initDbRecoreds(long timeAfterReboot, long steps) {
    Date first = new Date(this.startOfToday() - 1000);
    Date reboot = new Date(System.currentTimeMillis() - timeAfterReboot - 1000);

    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");

    String firstDate = fmt.format(first);
    String rebootDate = fmt.format(reboot);

    stepDao.insertStep(new StepLog(first.getTime(), (int) steps, -1));
    if (rebootDate.compareTo(firstDate) < 0) {
      stepDao.insertStep(new StepLog(reboot.getTime(), 0, -1));
    }
  }

  private void saveSteps(int steps, long timeAfterReboot) {
    if (steps < 0) {
      steps = 0;
    }
    long now = System.currentTimeMillis();
    if (now - lastPersistTime > STEP_PERSIST_DELAY_MS) {
      this.stepDao.insertStep(new StepLog(now, steps, timeAfterReboot));
      lastPersistTime = now;

      Log.i(TAG, "step saved: " + steps + " / " + now);
    }
  }

  private void fetchRebootNumSteps(long timeAfterBoot) {
    this.rebootNumSteps = 0;
    // load latest steps before reboot
    StepLog stepLog = stepDao.getLatestStep(System.currentTimeMillis() - timeAfterBoot);
    if (stepLog != null) {
      this.rebootNumSteps = stepLog.getStep();
    }
    Log.i(TAG, "step rebootNums: " + rebootNumSteps + "/" + (System.currentTimeMillis() - timeAfterBoot));
  }

  private void doStartAt(long startStamp) {
    this.startAt = startStamp;
    this.numSteps = 0;
    // startNumSteps の特別処理必要(DB記録ない時)
    this.startNumSteps = -1;
    StepLog stepLog = stepDao.getLatestStep(startStamp);
    if (stepLog != null) {
      this.startNumSteps = stepLog.getStep();
    }

    Log.i(TAG, "step startStamp: " + startStamp);
    Log.i(TAG, "step startNumSteps: " + startNumSteps);
  }

  private int fetchLatestSteps(long stamp) {
    int step = 0;
    // load latest steps before stamp
    StepLog stepLog = stepDao.getLatestStep(stamp);
    if (stepLog != null) {
      step = stepLog.getStep();
    }
    return step;
  }

  private void doExpire() {
    try {
      long clearLimit = System.currentTimeMillis() - BMDPedometerModule.LIMIT_OF_CLEAR_MS;
      long expire = System.currentTimeMillis() - BMDPedometerModule.LIMIT_OF_LOG_MS;
      if (clearLimit > this.clearAt) {
        this.stepDao.removeExpiredSteps(expire);
        this.stepDao.insertClearLog(new ClearLog(CLEAR_LOG_ID, System.currentTimeMillis()));
        this.clearAt = System.currentTimeMillis();

        Log.i(TAG, "step doExpire: done");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Start listening for pedometers sensor.
   */
  private void start() {
    Log.d(TAG, "start - entry");
    // If already starting or running, then return
    if ((this.status == BMDPedometerModule.RUNNING) || (this.status == BMDPedometerModule.STARTING)) {
      return;
    }

    this.setStatus(BMDPedometerModule.STARTING);

    // Get pedometer or accelerometer from sensor manager
    if (this.sensorManager == null) {
      this.setStatus(BMDPedometerModule.ERROR_FAILED_TO_START);
      Log.e(TAG, "START ERR: null sensorManager");
      return;
    }

    if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
      Log.d(TAG, "START ERR: permission denied");
    }

    this.mSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    if (this.mSensor == null) {
      Log.d(TAG, "try acc");
      this.mSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    // If found, then register as listener
    if (this.mSensor != null) {
      Log.d(TAG, "sensor type" + this.mSensor.getType());
      int sensorDelay = this.mSensor.getType() == Sensor.TYPE_STEP_COUNTER ? SensorManager.SENSOR_DELAY_UI : SensorManager.SENSOR_DELAY_FASTEST;
      if (!this.sensorManager.registerListener(this, this.mSensor, sensorDelay)) {
        this.setStatus(BMDPedometerModule.ERROR_FAILED_TO_START);
        return;
      };

      if (this.mSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        this.startAclSteps = this.fetchLatestSteps(System.currentTimeMillis());
        this.numSteps = this.startAclSteps - (this.startNumSteps < 0 ? 0 : this.startNumSteps);
        if (this.numSteps < 0) {
          this.numSteps = 0;
        }

        Log.i(TAG, "step numSteps: " + numSteps);
        Log.i(TAG, "step startAclSteps: " + startAclSteps);
      }
    } else {
      this.setStatus(BMDPedometerModule.ERROR_FAILED_TO_START);
      return;
    }
  }

  /**
   * Stop listening to sensor.
   */
  private void stop() {
    if (this.status != BMDPedometerModule.STOPPED) {
      if (this.sensorManager != null) {
        this.sensorManager.unregisterListener(this);
      }
    }
    this.setStatus(BMDPedometerModule.STOPPED);
  }

  private void setStatus(int status) {
    this.status = status;
  }

  private WritableMap getStepsParamsMap() {
    WritableMap map = Arguments.createMap();
    map.putDouble("startDate", this.startAt);
    map.putDouble("endDate", System.currentTimeMillis());
    map.putInt("numberOfSteps", this.numSteps);
    map.putDouble("distance", this.numSteps * BMDPedometerModule.STEP_IN_METERS);

    return map;
  }

  private WritableMap getErrorParamsMap(int code, String message) {
    // Error object
    WritableMap map = Arguments.createMap();
    try {
      map.putInt("code", code);
      map.putString("message", message);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return map;
  }

  private void sendPedometerUpdateEvent(@Nullable WritableMap params) {
    if (!reactContext.hasActiveCatalystInstance()) {
      return;
    }

    this.reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("pedometerDataDidUpdate", params);
  }
}
