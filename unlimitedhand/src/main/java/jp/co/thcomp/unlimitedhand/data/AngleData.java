package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;

public class AngleData extends AbstractSensorIntegerData {
    public static final boolean IS_SUPPORT_CALIBRATION = true;
    public static final int ANGLE_NUM = 3;

    public AngleData() {
        super();
    }

    public AngleData(AbstractSensorData srcSensorData) {
        super(srcSensorData);
    }

    @Override
    public int getSensorNum() {
        return ANGLE_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }

    @Override
    public Integer getRawValue(int channelNum) {
        Integer ret = super.getRawValue(channelNum);

        if (ret != null) {
            if ((-180 > ret) || (ret > 180)) {
                // invalid value, return 0
                ret = 0;
            }
        }

        return ret;
    }

    public Integer getRawValue(UhAccessHelper.Axis axis) {
        return getRawValue(axis.ordinal());
    }
}
