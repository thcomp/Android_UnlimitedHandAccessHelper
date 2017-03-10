package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.CalibrationData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.util.ThreadUtil;

public class UhGestureDetector {
    public enum WearDevice {
        RightArm,
        LeftArm,
        RightFoot,
        LeftFoot,;
    }

    public interface OnGestureListener {
        void onHandStatusChanged(HandData data);
    }

    public enum FingerCondition {
        Straight,
        SoftCurve,
        HardCurve,
    }

    ;

    public static class HandData {
        public FingerCondition thumb;
        public FingerCondition index;
        public FingerCondition middle;
        public FingerCondition ring;
        public FingerCondition pinky;

        public HandData(FingerCondition baseCondition) {
            thumb = index = middle = ring = pinky = baseCondition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HandData handData = (HandData) o;

            if (thumb != handData.thumb) return false;
            if (index != handData.index) return false;
            if (middle != handData.middle) return false;
            if (ring != handData.ring) return false;
            return pinky == handData.pinky;
        }

        @Override
        public int hashCode() {
            int result = thumb != null ? thumb.hashCode() : 0;
            result = 31 * result + (index != null ? index.hashCode() : 0);
            result = 31 * result + (middle != null ? middle.hashCode() : 0);
            result = 31 * result + (ring != null ? ring.hashCode() : 0);
            result = 31 * result + (pinky != null ? pinky.hashCode() : 0);
            return result;
        }
    }

    ;

    public static final int DEFAULT_CALIBRATE_INTERVAL_MS = 5 * 1000;
    public static final int DEFAULT_CALIBRATE_RATE_PER_SECOND = 30;
    private static final int DEFAULT_DETECT_THRESHOLD = 10;
    private static final String TAG = UhGestureDetector.class.getSimpleName();

    private Context mContext;
    private UhAccessHelper mUhAccessHelper;
    private OnGestureListener mOnGestureListener;
    private GestureDetector mGestureDetector = new GestureDetector();
    private WearDevice mWearDevice = WearDevice.RightArm;
    private HandData mLastHandData;
    private int mDetectThreshold = DEFAULT_DETECT_THRESHOLD;

    public UhGestureDetector(Context context, UhAccessHelper uhAccessHelper, WearDevice wearDevice) {
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
    }

    public void setGestureListener(OnGestureListener listener) {
        mOnGestureListener = listener;
    }

    public void startGestureListening() {
        if (!mGestureDetector.isListening()) {
            mGestureDetector.startGestureListening();
        }
    }

    public void stopGestureListening() {
        if (mGestureDetector.isListening()) {
            mGestureDetector.stopGestureListening();
        }
    }

    public void setDetectThreshold(int detectThreshold) {
        mDetectThreshold = detectThreshold;
    }

    public void startCalibration(Context context, CalibrationCondition calibrationCondition, final OnCalibrationStatusChangeListener listener) {
        mUhAccessHelper.startCalibration(context, calibrationCondition, listener);
    }

    public void stopCalibration() {
        mUhAccessHelper.stopCalibration();
    }

    private HandData detectGestureBaseOnHandOpen(PhotoReflectorData photoReflectorData, CalibrationData data) {
        HandData handData = new HandData(FingerCondition.Straight);
        List<Integer> overThresholdPRPositionList = getOverThresholdPRPositionList(photoReflectorData, data);

        if (overThresholdPRPositionList.size() == 0) {
            // conditionと完全一致
        } else {
            switch (mWearDevice) {
                case RightArm:
                    for (Integer position : overThresholdPRPositionList) {
                        UhAccessHelper.PhotoReflector pr = UhAccessHelper.PhotoReflector.values()[position];
                        switch (pr) {
                            case PR_0:
                                handData.thumb = FingerCondition.SoftCurve;
                                break;
                            case PR_1:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_2:
                                handData.middle = FingerCondition.SoftCurve;
                                break;
                            case PR_3:
                                handData.ring = FingerCondition.SoftCurve;
                                handData.pinky = FingerCondition.SoftCurve;
                                break;
                            case PR_4:
                            case PR_5:
                            case PR_6:
                            case PR_7:
                                // HandOpen時はこれらのPhoto-Reflectorの動作はハンドリングしない
                                break;
                        }
                    }
                    break;
                case LeftArm:
                    for (Integer position : overThresholdPRPositionList) {
                        UhAccessHelper.PhotoReflector pr = UhAccessHelper.PhotoReflector.values()[position];
                        switch (pr) {
                            case PR_0:
                                handData.ring = FingerCondition.SoftCurve;
                                handData.pinky = FingerCondition.SoftCurve;
                                break;
                            case PR_1:
                                handData.middle = FingerCondition.SoftCurve;
                                break;
                            case PR_2:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_3:
                                handData.thumb = FingerCondition.SoftCurve;
                                break;
                            case PR_4:
                            case PR_5:
                            case PR_6:
                            case PR_7:
                                // HandOpen時はこれらのPhoto-Reflectorの動作はハンドリングしない
                                break;
                        }
                    }
                    break;
            }
        }

        return handData;
    }

    private HandData detectGestureBaseOnHandClose(PhotoReflectorData photoReflectorData, CalibrationData data) {
        HandData handData = new HandData(FingerCondition.HardCurve);
        List<Integer> overThresholdPRPositionList = getOverThresholdPRPositionList(photoReflectorData, data);

        if (overThresholdPRPositionList.size() == 0) {
            // conditionと完全一致
        } else {
            switch (mWearDevice) {
                case RightArm:
                    for (Integer position : overThresholdPRPositionList) {
                        UhAccessHelper.PhotoReflector pr = UhAccessHelper.PhotoReflector.values()[position];
                        switch (pr) {
                            case PR_0:
                                handData.thumb = FingerCondition.SoftCurve;
                                break;
                            case PR_1:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_2:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_3:
                                handData.ring = FingerCondition.SoftCurve;
                                handData.pinky = FingerCondition.SoftCurve;
                                break;
                            case PR_4:
                            case PR_5:
                            case PR_6:
                            case PR_7:
                                // HandClose時はこれらのPhoto-Reflectorの動作はハンドリングしない
                                break;
                        }
                    }
                    break;
                case LeftArm:
                    for (Integer position : overThresholdPRPositionList) {
                        UhAccessHelper.PhotoReflector pr = UhAccessHelper.PhotoReflector.values()[position];
                        switch (pr) {
                            case PR_0:
                                handData.ring = FingerCondition.SoftCurve;
                                handData.pinky = FingerCondition.SoftCurve;
                                break;
                            case PR_1:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_2:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_3:
                                handData.thumb = FingerCondition.SoftCurve;
                                break;
                            case PR_4:
                            case PR_5:
                            case PR_6:
                            case PR_7:
                                // HandClose時はこれらのPhoto-Reflectorの動作はハンドリングしない
                                break;
                        }
                    }
                    break;
            }
        }

        return handData;
    }

    private HandData detectGestureBaseOnPickObject(PhotoReflectorData photoReflectorData, CalibrationData data) {
        HandData handData = new HandData(FingerCondition.SoftCurve);
        List<Integer> overThresholdPRPositionList = getOverThresholdPRPositionList(photoReflectorData, data);

        if (overThresholdPRPositionList.size() == 0) {
            // conditionと完全一致
        } else {
            switch (mWearDevice) {
                case RightArm:
                    for (Integer position : overThresholdPRPositionList) {
                        UhAccessHelper.PhotoReflector pr = UhAccessHelper.PhotoReflector.values()[position];
                        switch (pr) {
                            case PR_0:
                                handData.thumb = FingerCondition.SoftCurve;
                                break;
                            case PR_1:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_2:
                                handData.index = FingerCondition.SoftCurve;
                                break;
                            case PR_3:
                                handData.ring = FingerCondition.SoftCurve;
                                handData.pinky = FingerCondition.SoftCurve;
                                break;
                            case PR_4:
                            case PR_5:
                            case PR_6:
                            case PR_7:
                                // PickObject時はこれらのPhoto-Reflectorの動作はハンドリングしない
                                break;
                        }
                    }
                    break;
            }
        }

        return handData;
    }

    private List<Integer> getOverThresholdPRPositionList(PhotoReflectorData photoReflectorData, CalibrationData calibrationData) {
        ArrayList<Integer> overThresholdIndexList = new ArrayList<Integer>();

        // 閾値を超えたPhoto-Reflectorのインデックス番号を保持
        for (int i = 0, size = photoReflectorData.getSensorNum(); i < size; i++) {
            if (Math.abs(calibrationData.mPRAveArray[i] - photoReflectorData.getRawValue(i)) > mDetectThreshold) {
                overThresholdIndexList.add(i);
            }
        }

        return overThresholdIndexList;
    }

    private class GestureDetector implements UhAccessHelper.OnSensorPollingListener {
        private boolean mListening = false;

        public void startGestureListening() {
            if (!mListening) {
                mListening = true;
                mUhAccessHelper.startPollingSensor(this, UhAccessHelper.POLLING_PHOTO_REFLECTOR);
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

        @Override
        public void onPollSensor(AbstractSensorData[] sensorDataArray) {
            final OnGestureListener listener = mOnGestureListener;

            if (mListening && (listener != null)) {
                HashMap<CalibrationCondition, CalibrationData> calibrationDataMap = mUhAccessHelper.getCalibrationDataMap();
                CalibrationCondition[] keyArray = calibrationDataMap.keySet().toArray(new CalibrationCondition[0]);
                CalibrationData[] valueArray = calibrationDataMap.values().toArray(new CalibrationData[0]);

                for (AbstractSensorData sensorData : sensorDataArray) {
                    if (sensorData instanceof PhotoReflectorData) {
                        PhotoReflectorData photoReflectorData = (PhotoReflectorData) sensorData;
                        int[] diffSizeArrayPerCondition = new int[valueArray.length];
                        int minDiffSize = Integer.MAX_VALUE;
                        int minDiffSizeIndex = Integer.MAX_VALUE;

                        for (int i = 0, size = valueArray.length; i < size; i++) {
                            CalibrationData calibrationData = valueArray[i];
                            int tempMaxDiffSize = 0;

                            for (int j = 0, sensorSize = photoReflectorData.getSensorNum(); j < sensorSize; j++) {
                                // 各Calibrationを行った状態との乖離を保存
                                diffSizeArrayPerCondition[i] += Math.abs(calibrationData.mPRAveArray[j] - photoReflectorData.getRawValue(j));
                            }

                            if (minDiffSize > diffSizeArrayPerCondition[i]) {
                                minDiffSize = diffSizeArrayPerCondition[i];
                                minDiffSizeIndex = i;
                            }
                        }

                        if (minDiffSizeIndex < valueArray.length) {
                            CalibrationCondition calibrationCondition = keyArray[minDiffSizeIndex];
                            CalibrationData calibrationData = valueArray[minDiffSizeIndex];
                            HandData handData = null;

                            // 見つかったCalibrationの状態をベースにして、閾値を超えるPhoto-Reflectorから状態を推測する
                            switch (calibrationCondition.handStatus) {
                                case HandClose:
                                    handData = detectGestureBaseOnHandClose(photoReflectorData, calibrationData);
                                    break;
                                case HandOpen:
                                    handData = detectGestureBaseOnHandOpen(photoReflectorData, calibrationData);
                                    break;
                                case PickObject:
                                    handData = detectGestureBaseOnPickObject(photoReflectorData, calibrationData);
                                    break;
                            }

                            if (handData != null) {
                                if (mLastHandData == null || !mLastHandData.equals(handData)) {
                                    final HandData fHandData = handData;
                                    ThreadUtil.runOnMainThread(mContext, new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.onHandStatusChanged(fHandData);
                                        }
                                    });
                                }

                                mLastHandData = handData;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
