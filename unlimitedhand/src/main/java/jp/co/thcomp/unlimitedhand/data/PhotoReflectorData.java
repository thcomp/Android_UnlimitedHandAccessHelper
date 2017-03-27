package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;

public class PhotoReflectorData extends AbstractSensorIntegerData {
    public static final boolean IS_SUPPORT_CALIBRATION = true;
    public static final int PHOTO_REFLECTOR_NUM = 8;

    @Override
    public int getSensorNum() {
        return PHOTO_REFLECTOR_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }

    @Override
    public String getRawDataSeparator() {
        return "_";
    }

    public Integer getRawValue(UhAccessHelper.PhotoReflector photoReflector) {
        return getRawValue(photoReflector.ordinal());
    }
}
