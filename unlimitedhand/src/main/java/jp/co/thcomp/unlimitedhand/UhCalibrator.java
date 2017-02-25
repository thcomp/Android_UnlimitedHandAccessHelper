package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import java.util.Arrays;

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
    private static final int MAX_CALIBRATION_AVERAGE_COUNT = 30;
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
    private int mPRAveCount = 1;
    private int mAccelAveCount = 1;
    private int mGyroAveCount = 1;
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
            ;
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
            data.mAccelTempGyroAveArray = Arrays.copyOf(mCalibrationData.mAccelTempGyroAveArray, mCalibrationData.mAccelTempGyroAveArray.length);
            ret = true;
        }

        return ret;
    }

    @Override
    public void onPollSensor(AbstractSensorData[] sensorDataArray) {
        CalibrationData data = mCalibrationData;
        if (data == null) {
            data = mCalibrationData = new CalibrationData();
        }
        LogUtil.d(TAG, "Calibration Data: " + data);

        for (AbstractSensorData sensorData : sensorDataArray) {
            if (sensorData instanceof PhotoReflectorData) {
                PhotoReflectorData photoReflectorData = (PhotoReflectorData) sensorData;

                for (int i = 0; i < PhotoReflectorData.PHOTO_REFLECTOR_NUM; i++) {
                    mPhotoReflectorSums[i] += photoReflectorData.getRawValue(i);
                    data.mPRAveArray[i] = mPhotoReflectorSums[i] / mPRAveCount;
                }
                mPRAveCount++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < mPRAveCount) {//reset the mPRAveCount
                    for (int i = 0; i < PhotoReflectorData.PHOTO_REFLECTOR_NUM; i++) {
                        mPhotoReflectorSums[i] = data.mPRAveArray[i];
                    }
                    mPRAveCount = 1;
                }
            } else if (sensorData instanceof AccelerationData) {
                AccelerationData accelerationData = (AccelerationData) sensorData;

                for (int i = 0; i < AccelerationData.ACCELERATION_NUM; i++) {
                    mAccelerationSums[i] += accelerationData.getRawValue(i);
                    data.mAccelTempGyroAveArray[i] = mAccelerationSums[i] / mAccelAveCount;
                }
                mAccelAveCount++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < mAccelAveCount) {//reset the mAccelAveCount
                    for (int i = 0; i < AccelerationData.ACCELERATION_NUM; i++) {
                        mAccelerationSums[i] = data.mAccelTempGyroAveArray[i];
                    }
                    mAccelAveCount = 1;
                }
            } else if (sensorData instanceof GyroData) {
                GyroData gyroData = (GyroData) sensorData;
                int baseIndex = AccelerationData.ACCELERATION_NUM + GyroData.SEPARATE_DATA_NUM;

                for (int i = baseIndex; i < GyroData.ACCELERATION_GYRO_NUM; i++) {
                    mGyroSums[i - baseIndex] += gyroData.getRawValue(i - baseIndex);
                    data.mAccelTempGyroAveArray[i] = mGyroSums[i - baseIndex] / mGyroAveCount;
                }
                mGyroAveCount++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < mGyroAveCount) {//reset the mGyroAveCount
                    for (int i = baseIndex; i < GyroData.ACCELERATION_GYRO_NUM; i++) {
                        mGyroSums[i - baseIndex] = data.mAccelTempGyroAveArray[i];
                    }
                    mGyroAveCount = 1;
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
            mOnetimeSemaphore.stop();
        }
    }

    private Runnable mCalibrationRunnable = new Runnable() {
        @Override
        public void run() {
            int pollingSensors = 0;
            if (PhotoReflectorData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_PHOTO_REFLECTOR;
            }
            if (AngleData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_ANGLE;
            }
            if (TemperatureData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_TEMPERATURE;
            }
            if (AccelerationData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_ACCELERATION;
            }
            if (GyroData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_GYRO;
            }
            if (QuaternionData.IS_SUPPORT_CALIBRATION) {
                pollingSensors |= UhAccessHelper.POLLING_QUATERNION;
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

            mUhAccessHelper.stopPollingSensor(UhCalibrator.this);

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
}

