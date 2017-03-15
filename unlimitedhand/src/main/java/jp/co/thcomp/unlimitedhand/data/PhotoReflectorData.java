package jp.co.thcomp.unlimitedhand.data;

public class PhotoReflectorData extends AbstractSensorIntegerData {
    public static final boolean IS_SUPPORT_CALIBRATION = true;
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
}
