package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.HandData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.util.ThreadUtil;

public class UhGestureDetector2 {
    private static final String INPUT_SENSOR_NAME = "sensor_values_placeholder:0";
    private static final String OUTPUT_GESTURE_NAME = "softmax_linear/add:0";

    static {
        System.loadLibrary("tensorflow_inference");
    }

    public static final int DEFAULT_CALIBRATE_RATE_PER_SECOND = 30;
    private static final int DEFAULT_DETECT_THRESHOLD = 10;
    private static final String TAG = UhGestureDetector.class.getSimpleName();

    private Context mContext;
    private UhAccessHelper mUhAccessHelper;
    private UhGestureDetector.OnFingerStatusListener mOnFingerStatusListener;
    private GestureDetector mGestureDetector = new GestureDetector();
    private UhGestureDetector.WearDevice mWearDevice = UhGestureDetector.WearDevice.RightArm;
    private long mNotifyIndex = 0;
    private TensorFlowInferenceInterface mInterface;

    public UhGestureDetector2(Context context, UhAccessHelper uhAccessHelper, UhGestureDetector.WearDevice wearDevice, String mlPbFile) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        if (uhAccessHelper == null) {
            throw new NullPointerException("uhAccessHelper == null");
        }
        if (wearDevice == null) {
            throw new NullPointerException("wearDevice == null");
        }

        mContext = context;
        mUhAccessHelper = uhAccessHelper;
        mWearDevice = wearDevice;

        // TensorFlow_NightlyBuild
        mInterface = new TensorFlowInferenceInterface(context.getAssets(), "file:///android_asset/" + mlPbFile);
        // TensorFlow_r1.0
        //mInterface = new TensorFlowInferenceInterface();
        //mInterface.initializeTensorFlow(context.getAssets(), "file:///android_asset/" + mlPbFile);
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
            mGestureDetector.startGestureListening();
        }
    }

    public void stopListening() {
        if (mGestureDetector.isListening()) {
            mGestureDetector.stopGestureListening();
        }
    }

    private class GestureDetector implements UhAccessHelper.OnSensorPollingListener {
        private boolean mListening = false;

        public void startGestureListening() {
            if (!mListening) {
                mListening = true;
                mUhAccessHelper.setPollingRatePerSecond(DEFAULT_CALIBRATE_RATE_PER_SECOND);
                mUhAccessHelper.startPollingSensor(this, UhAccessHelper.POLLING_PHOTO_REFLECTOR | UhAccessHelper.POLLING_ACCELERATION | UhAccessHelper.POLLING_GYRO);
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

                for (AbstractSensorData sensorData : sensorDataArray) {
                    if (sensorData instanceof PhotoReflectorData) {
                        photoReflectorData = (PhotoReflectorData) sensorData;
                    } else if (sensorData instanceof AccelerationData) {
                        accelerationData = (AccelerationData) sensorData;
                    } else if (sensorData instanceof GyroData) {
                        gyroData = (GyroData) sensorData;
                    }
                }

                if ((fingerStatusListener != null) && (photoReflectorData != null) && (accelerationData != null) && (gyroData != null)) {
                    final AbstractSensorData[] fSensorDataArray = {
                            accelerationData,
                            gyroData,
                            photoReflectorData,
                    };
                    final int[] fSensorDataCountArray = {
                            AccelerationData.ACCELERATION_NUM,
                            GyroData.GYRO_NUM,
                            PhotoReflectorData.PHOTO_REFLECTOR_NUM,
                    };

                    ThreadUtil.runOnWorkThread(mContext, new Runnable() {
                        @Override
                        public void run() {
                            // create data
                            float[] sensorValueArray = new float[AccelerationData.ACCELERATION_NUM + GyroData.GYRO_NUM + PhotoReflectorData.PHOTO_REFLECTOR_NUM];
                            int dataPosition = 0;
                            for (int i = 0, sizeI = fSensorDataArray.length; i < sizeI; i++) {
                                for (int j = 0; j < fSensorDataCountArray[i]; j++) {
                                    Object tempValue = fSensorDataArray[i].getRawValue(j);
                                    if (tempValue instanceof Integer) {
                                        sensorValueArray[dataPosition] = (int) tempValue;
                                    } else if (tempValue instanceof Float) {
                                        sensorValueArray[dataPosition] = (float) tempValue;
                                    }
                                    dataPosition++;
                                }
                            }

                            try {
                                float[] inferenceResult = new float[(int) Math.pow(2, 5)];
                                // TensorFlow_NightlyBuild
                                mInterface.feed(INPUT_SENSOR_NAME, sensorValueArray, 1, sensorValueArray.length);
                                mInterface.run(new String[]{OUTPUT_GESTURE_NAME});
                                mInterface.fetch(OUTPUT_GESTURE_NAME, inferenceResult);
                                // TensorFlow_r1.0
                                //mInterface.fillNodeFloat(INPUT_SENSOR_NAME, new int[]{1, sensorValueArray.length}, sensorValueArray);
                                //mInterface.runInference(new String[]{OUTPUT_GESTURE_NAME});
                                //mInterface.readNodeFloat(OUTPUT_GESTURE_NAME, inferenceResult);

                                // 最も確率の高い値を選択
                                int highestValue = getMostProbabilityValue(inferenceResult);
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
                            } catch (Exception e) {
                                // 処理なし
                            }
                        }
                    });
                }
            }
        }
    }
}
