package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.LogUtil;

public class GyroData extends AbstractSensorFloatData {
    public static final boolean IS_SUPPORT_CALIBRATION = false;
    public static final int GYRO_NUM = 3;
    public static final int SEPARATE_DATA_NUM = 1;
    public static final int ACCELERATION_GYRO_NUM = AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + GyroData.GYRO_NUM;

    public GyroData() {
        super();
    }

    public GyroData(AbstractSensorData srcSensorData) {
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

        if (channelNum >= 0 && channelNum < GYRO_NUM) {
            ret = super.getRawValue(AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + channelNum);
        }

        return ret;
    }

    public Float getRawValue(UhAccessHelper.Axis axis) {
        return getRawValue(axis.ordinal());
    }
}
