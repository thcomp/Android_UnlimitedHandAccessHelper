package jp.co.thcomp.unlimitedhand;

import android.content.Context;
import android.os.AsyncTask;

import java.util.Arrays;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.CalibrationData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.ThreadUtil;

class UhCalibrator implements UhAccessHelper.OnSensorPollingListener {
    private static final int CALIBRATE_ANGLE_FLAT_AVE = 30;
    private static final String TAG = UhCalibrator.class.getSimpleName();

    private Context mContext;
    private UhAccessHelper mUhAccessHelper;
    private OnCalibrationStatusChangeListener mListener;
    private CalibrationData mCalibrationData;
    private ThreadUtil.OnetimeSemaphore mOnetimeSemaphore = new ThreadUtil.OnetimeSemaphore();
    private int mAngleFlatSize = 10;
    private int[] mAngleFlat = new int[mAngleFlatSize];
    private int mCount = 1;
    private boolean mDebug = false;
    private Thread mCalibrationThread;
    private CalibrationStatus mResultStatus = CalibrationStatus.Init;

    public UhCalibrator(Context context, UhAccessHelper uhAccessHelper, boolean debug) {
        mContext = context;
        mUhAccessHelper = uhAccessHelper;
        mDebug = debug;
    }

    public void setOnCalibrationStatusChangeListener(OnCalibrationStatusChangeListener listener) {
        mListener = listener;
    }

    public void startCalibration() {
        if (mCalibrationThread == null) {
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
            ret = true;
        }

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

        if (mDebug) {
            LogUtil.d(TAG, data.toString());
        }

        if (data.mAngleFlatAve > CALIBRATE_ANGLE_FLAT_AVE) {
            mOnetimeSemaphore.stop();
        }
    }

    private Runnable mCalibrationRunnable = new Runnable() {
        @Override
        public void run() {
            mUhAccessHelper.startPollingSensor(UhCalibrator.this, UhAccessHelper.POLLING_PHOTO_REFLECTOR | UhAccessHelper.POLLING_ANGLE);
            mOnetimeSemaphore.initialize();
            mOnetimeSemaphore.start();

            if (mCalibrationData != null) {
                if (mCalibrationData.mAngleFlatAve > CALIBRATE_ANGLE_FLAT_AVE) {
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

