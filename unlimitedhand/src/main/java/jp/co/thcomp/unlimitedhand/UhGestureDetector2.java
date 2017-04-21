package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Arrays;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.HandData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.unlimitedhand.data.QuaternionData;
import jp.co.thcomp.unlimitedhand.data.TemperatureData;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

public class UhGestureDetector2 {
    private static final String INPUT_SENSOR_NAME = "sensor_values_placeholder:0";
    private static final String INPUT_LABEL_NAME = "labels_placeholder:0";
    private static final String OUTPUT_GESTURE_NAME = "eval_correct";

    public static final int USE_SENSOR_INDEX_ACCELERATION = 0;
    public static final int USE_SENSOR_INDEX_GYRO = 1;
    public static final int USE_SENSOR_INDEX_PHOTO_REFLECTOR = 2;
    public static final int USE_SENSOR_INDEX_ANGLE = 3;
    public static final int USE_SENSOR_INDEX_TEMPERATURE = 4;
    public static final int USE_SENSOR_INDEX_QUATERNION = 5;
    public static final int MAX_USE_SENSOR = 6;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    public static final int DEFAULT_CALIBRATE_RATE_PER_SECOND = 30;
    private static final String TAG = UhGestureDetector2.class.getSimpleName();

    private Context mContext;
    private UhAccessHelper mUhAccessHelper;
    private UhGestureDetector.OnFingerStatusListener mOnFingerStatusListener;
    private GestureDetector mGestureDetector = new GestureDetector();
    private UhGestureDetector.WearDevice mWearDevice = UhGestureDetector.WearDevice.RightArm;
    private int mCombineSensorDataCount = 1;
    private long mNotifyIndex = 0;
    private TensorFlowInferenceInterface mInterface;
    private ArrayList<AbstractSensorData[]> mCombineSensorDatasList = new ArrayList<AbstractSensorData[]>();
    private boolean[] mUseEachSensor = new boolean[MAX_USE_SENSOR];

    public UhGestureDetector2(Context context, UhAccessHelper uhAccessHelper, UhGestureDetector.WearDevice wearDevice, String mlPbFile, int combineSensorDataCount) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        if (uhAccessHelper == null) {
            throw new NullPointerException("uhAccessHelper == null");
        }
        if (wearDevice == null) {
            throw new NullPointerException("wearDevice == null");
        }
        if (combineSensorDataCount <= 0) {
            throw new IllegalArgumentException("combineSensorDataCount = " + combineSensorDataCount);
        }

        mContext = context;
        mUhAccessHelper = uhAccessHelper;
        mWearDevice = wearDevice;
        mCombineSensorDataCount = combineSensorDataCount;

        // TensorFlow_NightlyBuild
        mInterface = new TensorFlowInferenceInterface(context.getAssets(), mlPbFile);
        // TensorFlow_r1.0
        //mInterface = new TensorFlowInferenceInterface();
        //mInterface.initializeTensorFlow(context.getAssets(), mlPbFile);
    }

    public void setFingerStatusListener(UhGestureDetector.OnFingerStatusListener listener) {
        mOnFingerStatusListener = listener;
        if (mGestureDetector.isListening()) {
            mGestureDetector.stopGestureListening();
            mGestureDetector.startGestureListening();
        }
    }

    public void startListening() {
        if (!mGestureDetector.isListening()) {
            mCombineSensorDatasList.clear();
            mGestureDetector.startGestureListening();
        }
    }

    public void stopListening() {
        if (mGestureDetector.isListening()) {
            mGestureDetector.stopGestureListening();
        }
    }

    public void useAccelerationSensor(boolean use) {
        mUseEachSensor[USE_SENSOR_INDEX_ACCELERATION] = use;
    }

    public void useGyroSensor(boolean use) {
        mUseEachSensor[USE_SENSOR_INDEX_GYRO] = use;
    }

    public void usePhotoReflectorSensor(boolean use) {
        mUseEachSensor[USE_SENSOR_INDEX_PHOTO_REFLECTOR] = use;
    }

    public void useAngleSensor(boolean use) {
        mUseEachSensor[USE_SENSOR_INDEX_ANGLE] = use;
    }

    public void useTemperatureSensor(boolean use) {
        mUseEachSensor[USE_SENSOR_INDEX_TEMPERATURE] = use;
    }

    public void useQuaternionSensor(boolean use) {
        mUseEachSensor[USE_SENSOR_INDEX_QUATERNION] = use;
    }

    private class GestureDetector implements UhAccessHelper.OnSensorPollingListener {
        private boolean mListening = false;

        public void startGestureListening() {
            if (!mListening) {
                mListening = true;
                mUhAccessHelper.setPollingRatePerSecond(DEFAULT_CALIBRATE_RATE_PER_SECOND);

                int pollingFlag = 0;
                for (int i = 0, size = mUseEachSensor.length; i < size; i++) {
                    switch (i) {
                        case USE_SENSOR_INDEX_ACCELERATION:
                            pollingFlag += UhAccessHelper.POLLING_ACCELERATION;
                            break;
                        case USE_SENSOR_INDEX_GYRO:
                            pollingFlag += UhAccessHelper.POLLING_GYRO;
                            break;
                        case USE_SENSOR_INDEX_PHOTO_REFLECTOR:
                            pollingFlag += UhAccessHelper.POLLING_PHOTO_REFLECTOR;
                            break;
                        case USE_SENSOR_INDEX_ANGLE:
                            pollingFlag += UhAccessHelper.POLLING_ANGLE;
                            break;
                        case USE_SENSOR_INDEX_TEMPERATURE:
                            pollingFlag += UhAccessHelper.POLLING_TEMPERATURE;
                            break;
                        case USE_SENSOR_INDEX_QUATERNION:
                            pollingFlag += UhAccessHelper.POLLING_QUATERNION;
                            break;
                    }
                }

                mUhAccessHelper.startPollingSensor(this, pollingFlag);
            }
        }

        public void stopGestureListening() {
            if (mListening) {
                mListening = false;
                mUhAccessHelper.stopPollingSensor(this);
            }
        }

        public boolean isListening() {
            return mListening;
        }

        private UhGestureDetector.FingerCondition getFingerCondition(int mlValue) {
            UhGestureDetector.FingerCondition ret = UhGestureDetector.FingerCondition.Straight;
            UhGestureDetector.FingerCondition[] conditions = UhGestureDetector.FingerCondition.values();

            for (int i = 0, size = conditions.length; i < size; i++) {
                if (mlValue <= i) {
                    ret = conditions[i];
                    break;
                }
            }

            return ret;
        }

        private int getMostProbabilityValue(float[] logits) {
            int ret = 0;
            float highestValue = Float.MIN_VALUE;

            for (int i = 0, size = logits.length; i < size; i++) {
                float probability = (float) (Math.pow(10, logits[i]) / (1 + Math.pow(10, logits[i])));
                if (probability > highestValue) {
                    highestValue = probability;
                    ret = i;
                }
            }

            return ret;
        }

        @Override
        public void onPollSensor(AbstractSensorData[] sensorDataArray) {
            final UhGestureDetector.OnFingerStatusListener fingerStatusListener = mOnFingerStatusListener;

            if (mListening) {
                PhotoReflectorData photoReflectorData = null;
                AccelerationData accelerationData = null;
                GyroData gyroData = null;
                AngleData angleData = null;
                TemperatureData temperatureData = null;
                QuaternionData quaternionData = null;

                for (AbstractSensorData sensorData : sensorDataArray) {
                    if (sensorData instanceof PhotoReflectorData) {
                        photoReflectorData = (PhotoReflectorData) sensorData;
                    } else if (sensorData instanceof AccelerationData) {
                        accelerationData = (AccelerationData) sensorData;
                    } else if (sensorData instanceof GyroData) {
                        gyroData = (GyroData) sensorData;
                    } else if (sensorData instanceof AngleData) {
                        angleData = (AngleData) sensorData;
                    } else if (sensorData instanceof TemperatureData) {
                        temperatureData = (TemperatureData) sensorData;
                    } else if (sensorData instanceof QuaternionData) {
                        quaternionData = (QuaternionData) sensorData;
                    }
                }

                if (fingerStatusListener != null) {
                    mCombineSensorDatasList.add(new AbstractSensorData[]{accelerationData, gyroData, photoReflectorData, angleData, temperatureData, quaternionData});

                    if (mCombineSensorDatasList.size() >= mCombineSensorDataCount) {
                        final int[] fSensorDataCountArray = {
                                AccelerationData.ACCELERATION_NUM,
                                GyroData.GYRO_NUM,
                                PhotoReflectorData.PHOTO_REFLECTOR_NUM,
                                AngleData.ANGLE_NUM,
                                TemperatureData.TEMPERATURE_NUM,
                                QuaternionData.QUATERNION_NUM,
                        };
                        ThreadUtil.runOnWorkThread(mContext, new Runnable() {
                            @Override
                            public void run() {
                                int sensorValueSize = 0;
                                for (int i = 0; i < MAX_USE_SENSOR; i++) {
                                    if (mUseEachSensor[i]) {
                                        sensorValueSize += fSensorDataCountArray[i];
                                    }
                                }
                                float[] sensorValueArray = new float[sensorValueSize * mCombineSensorDataCount];

                                synchronized (UhGestureDetector2.this) {
                                    // create data
                                    int dataPosition = 0;
                                    for (int i = 0; i < mCombineSensorDataCount; i++) {
                                        AbstractSensorData[] tempSensorDataArray = mCombineSensorDatasList.get(i);
                                        for (int j = 0, sizeJ = tempSensorDataArray.length; j < sizeJ; j++) {
                                            if (mUseEachSensor[j] && (tempSensorDataArray[j] != null)) {
                                                for (int k = 0; k < fSensorDataCountArray[j]; k++) {
                                                    Object tempValue = tempSensorDataArray[j].getRawValue(k);
                                                    if (tempValue instanceof Integer) {
                                                        sensorValueArray[dataPosition] = (int) tempValue;
                                                    } else if (tempValue instanceof Float) {
                                                        sensorValueArray[dataPosition] = (float) tempValue;
                                                    }
                                                    dataPosition++;
                                                }
                                            }
                                        }
                                    }

                                    // 先頭のデータは使用済みのため削除
                                    mCombineSensorDatasList.remove(0);
                                }

                                int[] inferenceResult = new int[1];
                                int value = Integer.MIN_VALUE;
                                boolean successFetch = true;

                                LogUtil.d(TAG, INPUT_SENSOR_NAME + ": " + Arrays.toString(sensorValueArray));
                                for (int i = 0, size = (int) Math.pow(2, 5); i < size; i++) {
                                    try {
                                        // TensorFlow_NightlyBuild
                                        mInterface.feed(INPUT_SENSOR_NAME, sensorValueArray, 1, sensorValueArray.length);
                                        mInterface.feed(INPUT_LABEL_NAME, new int[]{i}, 1);
                                        mInterface.run(new String[]{OUTPUT_GESTURE_NAME});
                                        mInterface.fetch(OUTPUT_GESTURE_NAME, inferenceResult);
                                        if (inferenceResult[0] != 0) {
                                            value = i;
                                            break;
                                        }
                                        // TensorFlow_r1.0
                                        //mInterface.fillNodeFloat(INPUT_SENSOR_NAME, new int[]{1, sensorValueArray.length}, sensorValueArray);
                                        //mInterface.runInference(new String[]{OUTPUT_GESTURE_NAME});
                                        //mInterface.readNodeFloat(OUTPUT_GESTURE_NAME, inferenceResult);
                                    } catch (Exception e) {
                                        successFetch = false;
                                        LogUtil.exception(TAG, e);
                                    }
                                }

                                if (successFetch) {
                                    int highestValue = value;
                                    int fingerConditionCount = UhGestureDetector.FingerCondition.values().length;
                                    final HandData fHandData = new HandData(UhGestureDetector.FingerCondition.Straight);
                                    int thumbCondition = (highestValue % (int) Math.pow(fingerConditionCount, 1));
                                    int indexCondition = (highestValue % (int) Math.pow(fingerConditionCount, 2)) / (int) Math.pow(fingerConditionCount, 1);
                                    int middleCondition = (highestValue % (int) Math.pow(fingerConditionCount, 3)) / (int) Math.pow(fingerConditionCount, 2);
                                    int ringCondition = (highestValue % (int) Math.pow(fingerConditionCount, 4)) / (int) Math.pow(fingerConditionCount, 3);
                                    int pinkyCondition = (highestValue % (int) Math.pow(fingerConditionCount, 5)) / (int) Math.pow(fingerConditionCount, 4);

                                    fHandData.thumb = getFingerCondition(thumbCondition);
                                    fHandData.index = getFingerCondition(indexCondition);
                                    fHandData.middle = getFingerCondition(middleCondition);
                                    fHandData.ring = getFingerCondition(ringCondition);
                                    fHandData.pinky = getFingerCondition(pinkyCondition);
                                    ThreadUtil.runOnMainThread(mContext, new Runnable() {
                                        @Override
                                        public void run() {
                                            fingerStatusListener.onFingerStatusChanged(mWearDevice, mNotifyIndex++, fHandData);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}
