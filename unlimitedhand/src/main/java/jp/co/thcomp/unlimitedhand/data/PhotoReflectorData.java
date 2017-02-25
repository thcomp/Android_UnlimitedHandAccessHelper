package jp.co.thcomp.unlimitedhand.data;

public class PhotoReflectorData extends AbstractSensorIntegerData {
    public static final boolean IS_SUPPORT_CALIBRATION = false;
    public static final int PHOTO_REFLECTOR_NUM = 8;

    @Override
    public int getSensorNum() {
        return PHOTO_REFLECTOR_NUM;
    }

    @Override
    protected String getRawDataSeparator() {
        return "_";
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
                calibratedChannelData[i] = String.valueOf(getRawValue(i) - calibrationData.mPRAveArray[i]);
            }
        }

        return ret;
    }
}
