package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.LogUtil;

public class AccelerationData extends AbstractSensorFloatData {
    public static final boolean IS_SUPPORT_CALIBRATION = true;
    public static final int ACCELERATION_NUM = 3;
    public static final int SEPARATE_DATA_NUM = 1;
    public static final int ACCELERATION_GYRO_NUM = AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + GyroData.GYRO_NUM;

    public AccelerationData() {
        super();
    }

    public AccelerationData(AbstractSensorData srcSensorData) {
        super(srcSensorData);
    }

    @Override
    public int getSensorNum() {
        return ACCELERATION_GYRO_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }

    @Override
    public Float getRawValue(int channelNum) {
        float ret = 0;

        if (channelNum >= 0 && channelNum < ACCELERATION_NUM) {
            ret = super.getRawValue(channelNum);
        }

        return ret;
    }

    public Float getRawValue(UhAccessHelper.Axis axis) {
        return getRawValue(axis.ordinal());
    }
}
