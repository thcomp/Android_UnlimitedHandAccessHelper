package jp.co.thcomp.unlimitedhand;

import android.content.Context;

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
    }

    public static final int DEFAULT_CALIBRATE_INTERVAL_MS = 5 * 1000;
    public static final int DEFAULT_CALIBRATE_RATE_PER_SECOND = 30;
    private static final String TAG = UhGestureDetector.class.getSimpleName();

    private UhAccessHelper mUhAccessHelper;
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

    public boolean startGestureListening() {
        boolean ret = false;

        if (mUhAccessHelper.isCalibrated()) {
            ret = true;

            if (!mGestureDetector.isListening()) {
                mGestureDetector.startGestureListening();
            }
        }

        return ret;
    }

    public void stopGestureListening() {
        if (mGestureDetector.isListening()) {
            mGestureDetector.stopGestureListening();
        }
    }

    public boolean isCalibrated() {
        return mUhAccessHelper.isCalibrated();
    }

    public void startCalibration(Context context, int calibrateDeviceAngle, final OnCalibrationStatusChangeListener listener) {
        mUhAccessHelper.startCalibration(context, calibrateDeviceAngle, listener);
    }

    public void stopCalibration() {
        mUhAccessHelper.stopCalibration();
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
            if (mListening) {
                for (AbstractSensorData sensorData : sensorDataArray) {
                    if (sensorData instanceof PhotoReflectorData) {
                    }
                }
            }
        }
    }
}
