package jp.co.thcomp.unlimitedhand;

import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.CalibrationData;
import jp.co.thcomp.unlimitedhand.data.HandData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

public class UhGestureDetector {
    public enum WearDevice {
        RightArm,
        LeftArm,
        RightFoot,
        LeftFoot,;
    }

    public interface OnFingerStatusListener {
        void onFingerStatusChanged(WearDevice wearDevice, long index, HandData data);
    }

    public interface OnDeviceAngleListener {
        void onDeviceAngleChanged(WearDevice wearDevice, long index, AngleData angleData);
    }

    public enum FingerCondition {
        Straight,
        //SoftCurve,
        HardCurve;

        static FingerCondition[] valuesReverse() {
            return new FingerCondition[]{
                    HardCurve,
                    //SoftCurve,
                    Straight};
        }
    }

    private enum PRValuePhase {
        NearBase,
        Middle,
        NearOpposite,
    }

    public static final int DEFAULT_CALIBRATE_INTERVAL_MS = 5 * 1000;
    public static final int DEFAULT_CALIBRATE_RATE_PER_SECOND = 30;
    private static final int DEFAULT_DETECT_THRESHOLD = 10;
    private static final float CHANGE_DETECT_RATE = 1.5f;
    private static final String TAG = UhGestureDetector.class.getSimpleName();

    private Context mContext;
    private UhAccessHelper mUhAccessHelper;
    private OnFingerStatusListener mOnFingerStatusListener;
    private OnDeviceAngleListener mOnDeviceAngleListener;
    private GestureDetector mGestureDetector = new GestureDetector();
    private WearDevice mWearDevice = WearDevice.RightArm;
    private HandData mLastHandData;
    private long mNotifyIndex = 0;
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

    public void setFingerStatusListener(OnFingerStatusListener listener) {
        mOnFingerStatusListener = listener;
        if (mGestureDetector.isListening()) {
            mGestureDetector.stopGestureListening();
            mGestureDetector.startGestureListening();
        }
    }

    public void setDeviceAngleListener(OnDeviceAngleListener listener) {
        mOnDeviceAngleListener = listener;
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

    public void setDetectThreshold(int detectThreshold) {
        mDetectThreshold = detectThreshold;
    }

    public int getDetectThreshold() {
        return mDetectThreshold;
    }

    public void startCalibration(Context context, CalibrationCondition calibrationCondition, final OnCalibrationStatusChangeListener listener) {
        mUhAccessHelper.startCalibration(context, calibrationCondition, listener);
    }

    public void stopCalibration() {
        mUhAccessHelper.stopCalibration();
    }

    private HandData detectGesture(PhotoReflectorData photoReflectorData, CalibrationData data, CalibrationData oppositeCalibrationData, FingerCondition[] arrayFingerCondition) {
        if (UhAccessHelper.isEnableDebug()) {
            LogUtil.d(TAG, this.getClass().getSimpleName() + ".detectGesture");
        }

        HandData handData = new HandData(arrayFingerCondition[PRValuePhase.NearBase.ordinal()]);
        HashMap<Integer, Integer> overThresholdPRPositionDiffMap = getOverThresholdPRPositionDiffMap(photoReflectorData, data);
        PRValuePhase[] allPrValuePhaseArray = new PRValuePhase[UhAccessHelper.PhotoReflector.values().length];

        for (UhAccessHelper.PhotoReflector pr : UhAccessHelper.PhotoReflector.values()) {
            int currentPrValue = photoReflectorData.getRawValue(pr.ordinal());
            int diffSensorValueInterCalibration = Math.abs(data.mPRAveArray[pr.ordinal()] - oppositeCalibrationData.mPRAveArray[pr.ordinal()]);
            int diffSensorValue = Math.abs(data.mPRAveArray[pr.ordinal()] - currentPrValue);

            if ((data.mPRAveArray[pr.ordinal()] < oppositeCalibrationData.mPRAveArray[pr.ordinal()]) && (currentPrValue < (data.mPRAveArray[pr.ordinal()] + diffSensorValueInterCalibration / 3))) {
                allPrValuePhaseArray[pr.ordinal()] = PRValuePhase.NearBase;
            } else if ((data.mPRAveArray[pr.ordinal()] > oppositeCalibrationData.mPRAveArray[pr.ordinal()]) && (currentPrValue > (data.mPRAveArray[pr.ordinal()] - diffSensorValueInterCalibration / 3))) {
                allPrValuePhaseArray[pr.ordinal()] = PRValuePhase.NearBase;
//            } else if ((data.mPRAveArray[pr.ordinal()] < oppositeCalibrationData.mPRAveArray[pr.ordinal()]) && (currentPrValue > (oppositeCalibrationData.mPRAveArray[pr.ordinal()] + diffSensorValueInterCalibration / 3))) {
//                allPrValuePhaseArray[pr.ordinal()] = PRValuePhase.NearOpposite;
//            } else if ((data.mPRAveArray[pr.ordinal()] > oppositeCalibrationData.mPRAveArray[pr.ordinal()]) && (currentPrValue > (data.mPRAveArray[pr.ordinal()] + diffSensorValueInterCalibration / 3))) {
//                allPrValuePhaseArray[pr.ordinal()] = PRValuePhase.NearOpposite;
            } else {
                allPrValuePhaseArray[pr.ordinal()] = diffSensorValue > (diffSensorValueInterCalibration * 2 / 3) ? PRValuePhase.NearOpposite : PRValuePhase.Middle;
            }
            if (UhAccessHelper.isEnableDebug()) {
                LogUtil.d(TAG, pr.name() + ": diff = " + overThresholdPRPositionDiffMap.get(pr.ordinal()) + "(" + Math.abs(data.mPRAveArray[pr.ordinal()] - oppositeCalibrationData.mPRAveArray[pr.ordinal()]) + "), PR Phase = " + allPrValuePhaseArray[pr.ordinal()].name());
            }
        }

        switch (mWearDevice) {
            case RightArm: {
                // PR_0(thumbとindexの間)
                allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()] = changePRValuePhase(null, allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()], allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()]);
                switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()]) {
                    case NearBase:
                        handData.thumb = arrayFingerCondition[allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()].ordinal()];
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()]) {
                            case NearBase:
                                handData.index = arrayFingerCondition[PRValuePhase.NearBase.ordinal()];
                                break;
                            case Middle:
                                handData.index = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.index = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case Middle:
                        handData.thumb = arrayFingerCondition[allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()].ordinal()];
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()]) {
                            case NearBase:
                                handData.index = arrayFingerCondition[PRValuePhase.NearBase.ordinal()];
                                break;
                            case Middle:
                                handData.index = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.index = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case NearOpposite:
                        handData.thumb = arrayFingerCondition[allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()].ordinal()];
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()]) {
                            case NearBase:
                                handData.index = arrayFingerCondition[PRValuePhase.NearBase.ordinal()];
                                break;
                            case Middle:
                                handData.index = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.index = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                }

                // PR_1(indexとmiddleの間)
                allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()] = changePRValuePhase(allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_0.ordinal()], allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()], allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()]);
                switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()]) {
                    case NearBase:
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()]) {
                            case NearBase:
                                handData.middle = arrayFingerCondition[PRValuePhase.NearBase.ordinal()];
                                break;
                            case Middle:
                                handData.middle = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.middle = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case Middle:
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()]) {
                            case NearBase:
                            case Middle:
                                handData.middle = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.middle = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case NearOpposite:
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()]) {
                            case NearBase:
                                handData.middle = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case Middle:
                            case NearOpposite:
                                handData.middle = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                }

                // PR_2(middleとringの間)
                allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()] = changePRValuePhase(allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_1.ordinal()], allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()], allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()]);
                switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()]) {
                    case NearBase:
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()]) {
                            case NearBase:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearBase.ordinal()];
                                break;
                            case Middle:
                                handData.ring = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case Middle:
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()]) {
                            case NearBase:
                            case Middle:
                                handData.ring = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case NearOpposite:
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()]) {
                            case NearBase:
                                handData.ring = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case Middle:
                            case NearOpposite:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                }

                // PR_3(ringとpinkyの間)
                allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()] = changePRValuePhase(allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_2.ordinal()], allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()], null);
                switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()]) {
                    case NearBase:
                        handData.pinky = arrayFingerCondition[allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()].ordinal()];
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_4.ordinal()]) {
                            case NearBase:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearBase.ordinal()];
                                break;
                            case Middle:
                                handData.ring = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case Middle:
                        handData.pinky = arrayFingerCondition[allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()].ordinal()];
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_4.ordinal()]) {
                            case NearBase:
                            case Middle:
                                handData.ring = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case NearOpposite:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                    case NearOpposite:
                        handData.pinky = arrayFingerCondition[allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_3.ordinal()].ordinal()];
                        switch (allPrValuePhaseArray[UhAccessHelper.PhotoReflector.PR_4.ordinal()]) {
                            case NearBase:
                                handData.ring = arrayFingerCondition[PRValuePhase.Middle.ordinal()];
                                break;
                            case Middle:
                            case NearOpposite:
                                handData.ring = arrayFingerCondition[PRValuePhase.NearOpposite.ordinal()];
                                break;
                        }
                        break;
                }
                break;
            }
            case LeftArm:
                break;
        }

        if (UhAccessHelper.isEnableDebug()) {
            LogUtil.d(TAG, "handData = " + handData);
        }

        return handData;
    }

    private PRValuePhase changePRValuePhase(PRValuePhase prevPRPhase, PRValuePhase targetPRPhase, PRValuePhase nextPRPhase) {
        PRValuePhase ret = targetPRPhase;

        if ((prevPRPhase != null) && (nextPRPhase != null)) {
            switch (targetPRPhase) {
                case NearBase:
                    if (prevPRPhase == PRValuePhase.NearOpposite && nextPRPhase == PRValuePhase.NearOpposite) {
                        ret = PRValuePhase.Middle;
                    }
                    break;
                case Middle:
                    break;
                case NearOpposite:
                    if (prevPRPhase == PRValuePhase.NearBase && nextPRPhase == PRValuePhase.NearBase) {
                        ret = PRValuePhase.Middle;
                    }
                    break;
            }
        } else if (prevPRPhase != null) {

        } else if (nextPRPhase != null) {

        }

        return ret;
    }

    private HashMap<Integer, Integer> getOverThresholdPRPositionDiffMap(PhotoReflectorData photoReflectorData, CalibrationData calibrationData) {
        HashMap<Integer, Integer> overThresholdIndexDiffMap = new HashMap<Integer, Integer>();

        // 閾値を超えたPhoto-Reflectorのインデックス番号を保持
        for (int i = 0, size = photoReflectorData.getSensorNum(); i < size; i++) {
            overThresholdIndexDiffMap.put(i, Math.abs(calibrationData.mPRAveArray[i] - photoReflectorData.getRawValue(i)));
        }

        return overThresholdIndexDiffMap;
    }

    private CalibrationData getOppositeCalibrationData(CalibrationCondition baseCalibrationCondition) {
        CalibrationData ret = null;
        CalibrationCondition.HandStatus oppositeHandStatus = null;

        switch (baseCalibrationCondition.handStatus) {
            case HandOpen:
                oppositeHandStatus = CalibrationCondition.HandStatus.HandClose;
                break;
            case HandClose:
            case PickObject:
                oppositeHandStatus = CalibrationCondition.HandStatus.HandOpen;
                break;
        }

        if (oppositeHandStatus != null) {
            HashMap<CalibrationCondition, CalibrationData> calibrationDataMap = mUhAccessHelper.getCalibrationDataMap();
            CalibrationCondition[] keyArray = calibrationDataMap.keySet().toArray(new CalibrationCondition[0]);
            CalibrationData[] valueArray = calibrationDataMap.values().toArray(new CalibrationData[0]);
            int diffDeviceAngleDegree = Integer.MAX_VALUE;

            // deviceの角度が一致するものまたは、それに最も近い角度のものを使用する
            for (int i = 0, size = keyArray.length; i < size; i++) {
                if (keyArray[i].handStatus == oppositeHandStatus) {
                    if (diffDeviceAngleDegree > Math.abs(baseCalibrationCondition.deviceAngleDegree - keyArray[i].deviceAngleDegree)) {
                        ret = valueArray[i];
                    }
                }
            }
        }

        return ret;
    }

    private class GestureDetector implements UhAccessHelper.OnSensorPollingListener {
        private boolean mListening = false;

        public void startGestureListening() {
            if (!mListening) {
                mListening = true;

                int pollingFlag = 0;
                if (mOnFingerStatusListener != null) {
                    pollingFlag |= UhAccessHelper.POLLING_PHOTO_REFLECTOR;
                }
                if (mOnDeviceAngleListener != null) {
                    pollingFlag |= UhAccessHelper.POLLING_ANGLE;
                }
                mUhAccessHelper.setPollingRatePerSecond(DEFAULT_CALIBRATE_RATE_PER_SECOND);
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

        @Override
        public void onPollSensor(AbstractSensorData[] sensorDataArray) {
            final OnFingerStatusListener fingerStatusListener = mOnFingerStatusListener;
            final OnDeviceAngleListener deviceAngleListener = mOnDeviceAngleListener;

            if (mListening) {
                HashMap<CalibrationCondition, CalibrationData> calibrationDataMap = mUhAccessHelper.getCalibrationDataMap();
                CalibrationCondition[] keyArray = calibrationDataMap.keySet().toArray(new CalibrationCondition[0]);
                CalibrationData[] valueArray = calibrationDataMap.values().toArray(new CalibrationData[0]);

                mNotifyIndex++;
                for (AbstractSensorData sensorData : sensorDataArray) {
                    if ((sensorData instanceof PhotoReflectorData) && (fingerStatusListener != null)) {
                        PhotoReflectorData photoReflectorData = (PhotoReflectorData) sensorData;
                        int[] diffSizeArrayPerCondition = new int[valueArray.length];
                        int minDiffSize = Integer.MAX_VALUE;
                        int minDiffSizeIndex = Integer.MAX_VALUE;

                        for (int i = 0, size = valueArray.length; i < size; i++) {
                            CalibrationData calibrationData = valueArray[i];

                            if (UhAccessHelper.isEnableDebug()) {
                                LogUtil.d(TAG, keyArray[i].handStatus.name() + ": Calibrated PR Ave: " + Arrays.toString(calibrationData.mPRAveArray) + ", Raw PR: " + photoReflectorData.toString());
                            }
                            for (int j = 0, sensorSize = photoReflectorData.getSensorNum(); j < sensorSize; j++) {
                                // 各Calibrationを行った状態との乖離を保存
                                diffSizeArrayPerCondition[i] += Math.abs(calibrationData.mPRAveArray[j] - photoReflectorData.getRawValue(j));
                            }

                            if (minDiffSize > diffSizeArrayPerCondition[i]) {
                                minDiffSize = diffSizeArrayPerCondition[i];
                                minDiffSizeIndex = i;
                            }
                        }
                        if (UhAccessHelper.isEnableDebug()) {
                            LogUtil.d(TAG, "All diff: " + Arrays.toString(diffSizeArrayPerCondition));
                        }

                        if (minDiffSizeIndex < valueArray.length) {
                            CalibrationCondition calibrationCondition = keyArray[minDiffSizeIndex];
                            CalibrationData calibrationData = valueArray[minDiffSizeIndex];
                            CalibrationData oppositeCalibrationData = getOppositeCalibrationData(calibrationCondition);
                            HandData handData = null;

                            // 見つかったCalibrationの状態をベースにして、閾値を超えるPhoto-Reflectorから状態を推測する
                            if (UhAccessHelper.isEnableDebug()) {
                                LogUtil.d(TAG, "InputData for detect: Photo-Reflector: " + photoReflectorData + ", Near calibration data: " + calibrationData + ", Far calibration data: " + oppositeCalibrationData);
                                LogUtil.d(TAG, this.getClass().getSimpleName() + ".detectGesture(" + calibrationCondition.handStatus.name());
                            }
                            switch (calibrationCondition.handStatus) {
                                case HandClose:
                                case PickObject:
                                    handData = detectGesture(photoReflectorData, calibrationData, oppositeCalibrationData, FingerCondition.valuesReverse());
                                    break;
                                case HandOpen:
                                    handData = detectGesture(photoReflectorData, calibrationData, oppositeCalibrationData, FingerCondition.values());
                                    break;
                            }

                            if (handData != null) {
                                if (mLastHandData == null || !mLastHandData.equals(handData)) {
                                    final HandData fHandData = handData;
                                    ThreadUtil.runOnMainThread(mContext, new Runnable() {
                                        @Override
                                        public void run() {
                                            fingerStatusListener.onFingerStatusChanged(mWearDevice, mNotifyIndex, fHandData);
                                        }
                                    });
                                }

                                mLastHandData = handData;
                            }
                        }
                    } else if ((sensorData instanceof AngleData) && (deviceAngleListener != null)) {
                        final AngleData fAngleData = new AngleData(sensorData);
                        ThreadUtil.runOnMainThread(mContext, new Runnable() {
                            @Override
                            public void run() {
                                deviceAngleListener.onDeviceAngleChanged(mWearDevice, mNotifyIndex, fAngleData);
                            }
                        });
                    }
                }
            }
        }
    }
}
