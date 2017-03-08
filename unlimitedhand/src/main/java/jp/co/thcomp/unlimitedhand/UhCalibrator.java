package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import java.lang.reflect.Field;
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
    private float[] mQuaternionSums = null;
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
            mQuaternionSums = new float[QuaternionData.QUATERNION_NUM];

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
            Field[] publicFields = CalibrationData.class.getFields();
            ret = true;

            if (publicFields != null && publicFields.length > 0) {
                for (Field publicField : publicFields) {
                    try {
                        if (java.lang.reflect.Modifier.isPrivate(publicField.getModifiers())) {
                            if (publicField.getType() == boolean.class) {
                                publicField.setBoolean(data, publicField.getBoolean(mCalibrationData));
                            } else if (publicField.getType() == byte.class) {
                                publicField.setByte(data, publicField.getByte(mCalibrationData));
                            } else if (publicField.getType() == char.class) {
                                publicField.setChar(data, publicField.getChar(mCalibrationData));
                            } else if (publicField.getType() == double.class) {
                                publicField.setDouble(data, publicField.getDouble(mCalibrationData));
                            } else if (publicField.getType() == float.class) {
                                publicField.setFloat(data, publicField.getFloat(mCalibrationData));
                            } else if (publicField.getType() == int.class) {
                                publicField.setInt(data, publicField.getInt(mCalibrationData));
                            } else if (publicField.getType() == long.class) {
                                publicField.setLong(data, publicField.getLong(mCalibrationData));
                            } else if (publicField.getType() == short.class) {
                                publicField.setShort(data, publicField.getShort(mCalibrationData));
                            }
                        } else {
                            Object publicFieldValue = publicField.get(mCalibrationData);

                            if (publicFieldValue instanceof Object[]) {
                                Object[] tempArray = (Object[]) publicFieldValue;
                                publicField.set(data, Arrays.copyOf(tempArray, tempArray.length));
                            } else {
                                publicField.set(data, publicField.get(mCalibrationData));
                            }
                        }
                    } catch (Exception e) {
                        ret = false;
                    }
                }
            }
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
                Float[] rawValueArray = new Float[AccelerationData.ACCELERATION_NUM];

                for (int i = 0; i < AccelerationData.ACCELERATION_NUM; i++) {
                    rawValueArray[i] = accelerationData.getRawValue(i);
                    mAccelerationSums[i] += rawValueArray[i];
                    data.mAccelGyroAveArray[i] = mAccelerationSums[i] / calibratingData.count;
                }
                if (UhAccessHelper.isEnableDebug()) {
                    try {
                        LogUtil.d(TAG, "Calibrating: mAccelerationSums = " + Arrays.toString(mAccelerationSums) + ", count = " + calibratingData.count + ", rawValueArray = " + Arrays.toString(rawValueArray));
                    } catch (Exception e) {
                        // 処理なし
                    }
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
                Float[] rawValueArray = new Float[GyroData.ACCELERATION_GYRO_NUM - baseIndex];

                for (int i = baseIndex; i < GyroData.ACCELERATION_GYRO_NUM; i++) {
                    rawValueArray[i - baseIndex] = gyroData.getRawValue(i - baseIndex);
                    mGyroSums[i - baseIndex] += rawValueArray[i - baseIndex];
                    data.mAccelGyroAveArray[i] = mGyroSums[i - baseIndex] / calibratingData.count;
                }
                if (UhAccessHelper.isEnableDebug()) {
                    try {
                        LogUtil.d(TAG, "Calibrating: mGyroSums = " + Arrays.toString(mGyroSums) + ", count = " + calibratingData.count + ", rawValueArray = " + Arrays.toString(rawValueArray));
                    } catch (Exception e) {
                        // 処理なし
                    }
                }
                calibratingData.count++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < calibratingData.count) {//reset the count
                    for (int i = baseIndex; i < GyroData.ACCELERATION_GYRO_NUM; i++) {
                        mGyroSums[i - baseIndex] = data.mAccelGyroAveArray[i];
                    }
                    calibratingData.count = 1;
                    calibratingData.enoughCount = true;
                }
            } else if (sensorData instanceof QuaternionData) {
                calibratingData = mCalibratingDataMap.get(QuaternionData.class);
                QuaternionData quaternionData = (QuaternionData) sensorData;

                for (int i = 0; i < QuaternionData.QUATERNION_NUM; i++) {
                    mQuaternionSums[i] += quaternionData.getRawValue(i);
                    data.mQuaternionAveArray[i] = mQuaternionSums[i] / calibratingData.count;
                }
                calibratingData.count++;

                if (MAX_CALIBRATION_AVERAGE_COUNT < calibratingData.count) {//reset the count
                    for (int i = 0; i < QuaternionData.QUATERNION_NUM; i++) {
                        mQuaternionSums[i] = data.mQuaternionAveArray[i];
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

