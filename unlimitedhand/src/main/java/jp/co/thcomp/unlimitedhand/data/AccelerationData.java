package jp.co.thcomp.unlimitedhand.data;

public class AccelerationData extends AbstractSensorFloatData {
    public static final int ACCELERATION_NUM = 3;
    private static final int SEPARATE_DATA_NUM = 1;
    private static final int ACCELERATION_GYRO_NUM = AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + GyroData.GYRO_NUM;

    @Override
    public int getSensorNum() {
        return ACCELERATION_GYRO_NUM;
    }

    @Override
    public Float getRawValue(int channelNum) {
        float ret = 0;

        if (channelNum >= 0 && channelNum < ACCELERATION_NUM) {
            ret = super.getRawValue(channelNum);
        }

        return ret;
    }
}
