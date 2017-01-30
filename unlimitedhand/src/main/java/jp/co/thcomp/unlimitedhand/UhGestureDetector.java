package jp.co.thcomp.unlimitedhand;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;

import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

public class UhGestureDetector {
    public enum WearDevice {
        RightArm,
        LeftArm,
        RightFoot,
        LeftFoot,;
    }

    public enum CalibrationStatus {
        Init,
        Calibrating,
        CalibrateSuccess,
        CalibrateFail,
    }

    public interface OnCalibrationStatusChangeListener {
        void onCalibrationStatusChange(CalibrationStatus status);
    }

    public interface OnGestureListener {
    }

    private static class CalibrationData {
        private int mAngleFlatAve = 0;
        private int[] mPRAveArray = new int[PhotoReflectorData.PHOTO_REFLECTOR_NUM];

        @Override
        public String toString() {
            return "CalibrationData{" +
                    "mAngleFlatAve=" + mAngleFlatAve +
                    ", mPRAveArray=" + Arrays.toString(mPRAveArray) +
                    '}';
        }
    }

    public static final int DEFAULT_CALIBRATE_INTERVAL_MS = 5 * 1000;
    public static final int DEFAULT_CALIBRATE_RATE_PER_SECOND = 30;
    private static final String TAG = UhGestureDetector.class.getSimpleName();
    private static final int CALIBRATE_ANGLE_FLAT_AVE = 30;

    public static void enableDebug(boolean enable) {
        sDebug = enable;
    }

    private static boolean sDebug = false;
    private CalibrationData mCalibrationData;
    private Calibrator mCalibrator;
    private CalibrationStatus mCalibrationStatus = CalibrationStatus.Init;
    private UhAccessHelper mUhAccessHelper;
    private OnCalibrationStatusChangeListener mOnCalibrationStatusChangeListener;
    private OnGestureListener mOnGestureListener;
    private GestureDetector mGestureDetector = new GestureDetector();
    private WearDevice mWearDevice = WearDevice.RightArm;

    public UhGestureDetector(UhAccessHelper uhAccessHelper, WearDevice wearDevice) {
        mUhAccessHelper = uhAccessHelper;
        mWearDevice = wearDevice;
    }

    public void setGestureListener(OnGestureListener listener) {
        mOnGestureListener = listener;
    }

    public void setOnCalibrationStatusChangeListener(OnCalibrationStatusChangeListener listener) {
        mOnCalibrationStatusChangeListener = listener;
    }

    public boolean isCalibrated() {
        return mCalibrationStatus == CalibrationStatus.CalibrateSuccess;
    }

    public void startCalibration(Context context) {
        synchronized (this) {
            switch (mCalibrationStatus) {
                case Init:
                case CalibrateFail:
                    mCalibrationStatus = CalibrationStatus.Calibrating;
                    mCalibrator = new Calibrator(mUhAccessHelper);
                    mCalibrator.setOnCalibrationStatusChangeListener(new OnCalibrationStatusChangeListener() {
                        @Override
                        public void onCalibrationStatusChange(CalibrationStatus status) {
                            try {
                                if (status == CalibrationStatus.CalibrateSuccess) {
                                    CalibrationData data = new CalibrationData();
                                    mCalibrator.getCalibrationData(data);
                                }

                                if (mOnCalibrationStatusChangeListener != null) {
                                    mOnCalibrationStatusChangeListener.onCalibrationStatusChange(status);
                                }
                            } finally {
                                mCalibrationStatus = status;
                                mCalibrator = null;
                            }
                        }
                    });
                    mCalibrator.execute();
                    break;
                case CalibrateSuccess:
                    if (mOnCalibrationStatusChangeListener != null) {
                        ThreadUtil.runOnMainThread(context, new Runnable() {
                            @Override
                            public void run() {
                                mOnCalibrationStatusChangeListener.onCalibrationStatusChange(mCalibrationStatus);
                            }
                        });
                    }
                    break;
                case Calibrating:
                default:
                    break;
            }
        }
    }

    public void stopCalibration() {
        synchronized (this) {
            if (mCalibrator != null) {
                mCalibrator.cancel(true);
            }
        }
    }

    public boolean startGestureListening() {
        boolean ret = false;

        if (isCalibrated()) {
            ret = true;

            if (!mGestureDetector.isListening()) {
                mGestureDetector.startGestureListening();
            }
        }

        return ret;
    }

    public void stopGestureListening() {

    }

    private static class Calibrator extends AsyncTask<Void, Void, CalibrationStatus> implements UhAccessHelper.OnSensorPollingListener {
        private UhAccessHelper mUhAccessHelper;
        private OnCalibrationStatusChangeListener mListener;
        private CalibrationData mCalibrationData;
        private ThreadUtil.OnetimeSemaphore mOnetimeSemaphore = new ThreadUtil.OnetimeSemaphore();
        private int mAngleFlatSize = 10;
        private int[] mAngleFlat = new int[mAngleFlatSize];
        private int mCount = 1;

        public Calibrator(UhAccessHelper uhAccessHelper) {
            mUhAccessHelper = uhAccessHelper;
        }

        public void setOnCalibrationStatusChangeListener(OnCalibrationStatusChangeListener listener) {
            mListener = listener;
        }

        public boolean getCalibrationData(CalibrationData data) {
            boolean ret = false;

            if (mCalibrationData != null) {
                data.mAngleFlatAve = mCalibrationData.mAngleFlatAve;
                data.mPRAveArray = Arrays.copyOf(mCalibrationData.mPRAveArray, mCalibrationData.mPRAveArray.length);
                ret = true;
            }

            return ret;
        }

        @Override
        protected CalibrationStatus doInBackground(Void... voids) {
            CalibrationStatus ret = CalibrationStatus.CalibrateFail;
            mUhAccessHelper.startPollingSensor(this, UhAccessHelper.POLLING_PHOTO_REFLECTOR | UhAccessHelper.POLLING_ANGLE);
            mOnetimeSemaphore.start();

            if (mCalibrationData != null) {
                if (mCalibrationData.mAngleFlatAve > CALIBRATE_ANGLE_FLAT_AVE) {
                    ret = CalibrationStatus.CalibrateSuccess;
                }
            }
            mUhAccessHelper.stopPollingSensor(this);

            return ret;
        }

        @Override
        public void onPollSensor(AbstractSensorData[] sensorDataArray) {
            CalibrationData data = mCalibrationData = new CalibrationData();

            for (AbstractSensorData sensorData : sensorDataArray) {
                if (sensorData instanceof PhotoReflectorData) {
                    PhotoReflectorData photoReflectorData = (PhotoReflectorData) sensorData;
                    int[] photoReflectorSums = new int[PhotoReflectorData.PHOTO_REFLECTOR_NUM];

                    for (int i = 0; i < PhotoReflectorData.PHOTO_REFLECTOR_NUM; i++) {
                        photoReflectorSums[i] += photoReflectorData.getValue(i);
                        data.mPRAveArray[i] = photoReflectorSums[i] / mCount;
                    }
                    mCount++;

                    if (30 < mCount) {//reset the mCount
                        for (int i = 0; i < PhotoReflectorData.PHOTO_REFLECTOR_NUM; i++) {
                            photoReflectorSums[i] = data.mPRAveArray[i];
                        }
                        mCount = 1;
                    }
                } else if (sensorData instanceof AngleData) {
                    AngleData angleData = (AngleData) sensorData;

                    for (int i = 1; i < mAngleFlatSize; i++) {
                        mAngleFlat[i - 1] = mAngleFlat[i];
                    }
                    mAngleFlat[mAngleFlatSize - 1] = angleData.getValue(0);
                    data.mAngleFlatAve = 0;
                    for (int i = 0; i < mAngleFlatSize; i++) {
                        data.mAngleFlatAve += mAngleFlat[i];
                    }
                    data.mAngleFlatAve = data.mAngleFlatAve / mAngleFlatSize;
                }
            }

            if (sDebug) {
                LogUtil.d(TAG, data.toString());
            }

            if (data.mAngleFlatAve > CALIBRATE_ANGLE_FLAT_AVE) {
                mOnetimeSemaphore.stop();
            } else if (isCancelled()) {
                mOnetimeSemaphore.start();
            }
        }

        @Override
        protected void onPostExecute(CalibrationStatus status) {
            super.onPostExecute(status);

            if (mListener != null) {
                mListener.onCalibrationStatusChange(status);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onPostExecute(CalibrationStatus.CalibrateFail);
        }

        @Override
        protected void onCancelled(CalibrationStatus status) {
            super.onCancelled(status);
            onPostExecute(status);
        }
    }

    private class GestureDetector implements UhAccessHelper.OnSensorPollingListener {
        private boolean mListening = false;
        private ThreadUtil.OnetimeSemaphore mOnetimeSemaphore = new ThreadUtil.OnetimeSemaphore();

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
            for (AbstractSensorData sensorData : sensorDataArray) {
                if (sensorData instanceof PhotoReflectorData) {
                }
            }
        }
    }
}
