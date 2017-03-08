package jp.co.thcomp.unlimitedhand.data;

public class QuaternionData extends AbstractSensorFloatData {
    public static final boolean IS_SUPPORT_CALIBRATION = false;
    public static final int QUATERNION_NUM = 4;

    @Override
    public int getSensorNum() {
        return QUATERNION_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }

    @Override
    public boolean calibrate(CalibrationData calibrationData) {
        boolean ret = super.calibrate(calibrationData);

        if (ret) {
            for (int i = 0, size = getSensorNum(); i < size; i++) {
                calibratedChannelData[i] = String.valueOf(getRawValue(i) - calibrationData.mQuaternionAveArray[i]);
            }
        }

        return ret;
    }
}
