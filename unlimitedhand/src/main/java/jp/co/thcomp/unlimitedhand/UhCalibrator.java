package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.CalibrationData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.unlimitedhand.data.QuaternionData;
import jp.co.thcomp.unlimitedhand.data.TemperatureData;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

class UhCalibrator implements UhAccessHelper.OnSensorPollingListener {
    private static final String TAG = UhCalibrator.class.getSimpleName();
    private static final int MAX_CALIBRATION_AVERAGE_COUNT = 10;
    private static final float CALIBRATION_AVERAGE_RANGE = 10f;

    private Context mContext;
    private UhAccessHelper mUhAccessHelper;
    private OnCalibrationStatusChangeListener mListener;
    private CalibrationData mCalibrationData;
    private ThreadUtil.OnetimeSemaphore mOnetimeSemaphore = new ThreadUtil.OnetimeSemaphore();
    private int mAngleFlatSize = 10;
    private int[] mAngleFlat = new int[mAngleFlatSize];
    private int[] mPhotoReflectorSums = null;
    private float[] mAccelerationSums = null;
    private float[] mGyroSums = null;
    //    private int mPRAveCount = 1;
//    private int mAccelAveCount = 1;
//    private int mGyroAveCount = 1;
    private HashMap<Class, CalibratingData> mCalibratingDataMap = new HashMap<Class, CalibratingData>();
    private boolean mDebug = false;
    private Thread mCalibrationThread;
    private CalibrationStatus mResultStatus = CalibrationStatus.Init;
    private int mCalibrateDeviceAngle = 0;

    public UhCalibrator(Context context, UhAccessHelper uhAccessHelper, boolean debug) {
        mContext = context;
        mUhAccessHelper = uhAccessHelper;
        mDebug = debug;
    }

    public void setOnCalibrationStatusChangeListener(OnCalibrationStatusChangeListener listener) {
        mListener = listener;
    }

    public void startCalibration(int calibrateDeviceAngle) {
        mCalibrateDeviceAngle = calibrateDeviceAngle;

        if (mCalibrationThread == null) {
            mPhotoReflectorSums = new int[PhotoReflectorData.PHOTO_REFLECTOR_NUM];
            mAccelerationSums = new float[AccelerationData.ACCELERATION_NUM];
            mGyroSums = new float[GyroData.GYRO_NUM];

            mCalibrationThread = new Thread(mCalibrationRunnable);
            mCalibrationThread.start();
        }
    }

    public void stopCalibration() {
        if (mCalibrationThread != null) {
            mOnetimeSemaphore.stop();
        }
    }

    public boolean getCalibrationData(CalibrationData data) {
        boolean ret = false;

        if (mCalibrationData != null) {
            data.mAngleFlatAve = mCalibrationData.mAngleFlatAve;
            data.mPRAveArray = Arrays.copyOf(mCalibrationData.mPRAveArray, mCalibrationData.mPRAveArray.length);
            data.mAccelGyroAveArray = Arrays.copyOf(mCalibrationData.mAccelGyroAveArray, mCalibrationData.mAccelGyroAveArray.length);
            ret = true;
        }

        return ret;
    }

    @Override
    public void onPollSensor(AbstractSensorData[] sensorDataArray) {
        CalibrationData data = mCalibrationData;
        CalibratingData calibratingData = null;
        if (data == null) {
            data = mCalibrationData = new CalibrationData();
        }
        LogUtil.d(TAG, "Calibration Data: " + data);

        for (AbstractSensorData sensorData : sensorDataArray) {
            if (sensorData instanceof PhotoReflectorData) {
                calibratingData = mCalibratingDataMap.get(PhotoReflectorData.class);
                PhotoReflectorData photoReflectorData = (PhotoReflectorData) sensorData;

                for (int i = 0; i < PhotoReflectorData.PHOTO_REFLECTOR_NUM; i++) {
                    mPhotoReflectorSums[i] += photoReflectorData.getRawValue(i);
                    data.mPRAveArray[i] = mPhotoReflectorSums[i] / calibratingData.count;
                }
                calibratingData.count++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < calibratingData.count) {//reset the mPRAveCount
                    for (int i = 0; i < PhotoReflectorData.PHOTO_REFLECTOR_NUM; i++) {
                        mPhotoReflectorSums[i] = data.mPRAveArray[i];
                    }
                    calibratingData.count = 1;
                    calibratingData.enoughCount = true;
                }
            } else if (sensorData instanceof AccelerationData) {
                calibratingData = mCalibratingDataMap.get(AccelerationData.class);
                AccelerationData accelerationData = (AccelerationData) sensorData;

                for (int i = 0; i < AccelerationData.ACCELERATION_NUM; i++) {
                    mAccelerationSums[i] += accelerationData.getRawValue(i);
                    data.mAccelGyroAveArray[i] = mAccelerationSums[i] / calibratingData.count;
                }
                calibratingData.count++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < calibratingData.count) {//reset the mAccelAveCount
                    for (int i = 0; i < AccelerationData.ACCELERATION_NUM; i++) {
                        mAccelerationSums[i] = data.mAccelGyroAveArray[i];
                    }
                    calibratingData.count = 1;
                    calibratingData.enoughCount = true;
                }
            } else if (sensorData instanceof GyroData) {
                calibratingData = mCalibratingDataMap.get(GyroData.class);
                GyroData gyroData = (GyroData) sensorData;
                int baseIndex = AccelerationData.ACCELERATION_NUM + GyroData.SEPARATE_DATA_NUM;

                for (int i = baseIndex; i < GyroData.ACCELERATION_GYRO_NUM; i++) {
                    mGyroSums[i - baseIndex] += gyroData.getRawValue(i - baseIndex);
                    data.mAccelGyroAveArray[i] = mGyroSums[i - baseIndex] / calibratingData.count;
                }
                calibratingData.count++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < calibratingData.count) {//reset the mGyroAveCount
                    for (int i = baseIndex; i < GyroData.ACCELERATION_GYRO_NUM; i++) {
                        mGyroSums[i - baseIndex] = data.mAccelGyroAveArray[i];
                    }
                    calibratingData.count = 1;
                    calibratingData.enoughCount = true;
                }
            } else if (sensorData instanceof AngleData) {
                AngleData angleData = (AngleData) sensorData;

                for (int i = 1; i < mAngleFlatSize; i++) {
                    mAngleFlat[i - 1] = mAngleFlat[i];
                }
                mAngleFlat[mAngleFlatSize - 1] = angleData.getRawValue(0);
                data.mAngleFlatAve = 0;
                for (int i = 0; i < mAngleFlatSize; i++) {
                    data.mAngleFlatAve += mAngleFlat[i];
                }
                data.mAngleFlatAve = data.mAngleFlatAve / mAngleFlatSize;
            }
        }

        if (mDebug) {
            LogUtil.d(TAG, data.toString());
        }

        // if average angle for calibration is in the range, calibration finishes
        if (Math.abs(data.mAngleFlatAve - mCalibrateDeviceAngle) <= CALIBRATION_AVERAGE_RANGE) {
            boolean enoughCalibrated = true;

            for (CalibratingData tempCalibratingData : mCalibratingDataMap.values()) {
                enoughCalibrated &= tempCalibratingData.enoughCount;
                if (!enoughCalibrated) {
                    break;
                }
            }

            if (enoughCalibrated) {
                // must stop polling sensor ASAP, because conflict read function causes dead lock.
                mUhAccessHelper.stopPollingSensor(UhCalibrator.this);
                mOnetimeSemaphore.stop();
            }
        }
    }

    private Runnable mCalibrationRunnable = new Runnable() {
        @Override
        public void run() {
            int pollingSensors = 0;
            mCalibratingDataMap.clear();

            if (PhotoReflectorData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_PHOTO_REFLECTOR;
                mCalibratingDataMap.put(PhotoReflectorData.class, new CalibratingData());
            }
            if (AngleData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_ANGLE;
                CalibratingData data = new CalibratingData();
                data.enoughCount = true;    // AngleData is judged with other condition.
                mCalibratingDataMap.put(AngleData.class, data);
            }
            if (TemperatureData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_TEMPERATURE;
                mCalibratingDataMap.put(TemperatureData.class, new CalibratingData());
            }
            if (AccelerationData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_ACCELERATION;
                mCalibratingDataMap.put(AccelerationData.class, new CalibratingData());
            }
            if (GyroData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_GYRO;
                mCalibratingDataMap.put(GyroData.class, new CalibratingData());
            }
            if (QuaternionData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_QUATERNION;
                mCalibratingDataMap.put(QuaternionData.class, new CalibratingData());
            }

            mUhAccessHelper.startPollingSensor(UhCalibrator.this, pollingSensors);
            mOnetimeSemaphore.initialize();
            mOnetimeSemaphore.start();

            if (mCalibrationData != null) {
                if (Math.abs(mCalibrationData.mAngleFlatAve - mCalibrateDeviceAngle) <= CALIBRATION_AVERAGE_RANGE) {
                    mResultStatus = CalibrationStatus.CalibrateSuccess;
                }
            } else {
                mResultStatus = CalibrationStatus.CalibrateFail;
            }

            ThreadUtil.runOnMainThread(mContext, new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onCalibrationStatusChange(mResultStatus);
                    }
                }
            });
            mCalibrationThread = null;
        }
    };

    private static class CalibratingData {
        int count = 1;
        boolean enoughCount = false;
    }
}

