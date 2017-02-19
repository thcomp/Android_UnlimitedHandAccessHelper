package jp.co.thcomp.unlimitedhand.data;

public class TemperatureData extends AbstractSensorFloatData {
    public static final int TEMPERATURE_NUM = 1;
    private static final int ANGLE_AND_TEMPERATURE_NUM = 4;

    @Override
    public int getSensorNum() {
        return ANGLE_AND_TEMPERATURE_NUM;
    }

    @Override
    public Float getRawValue(int channelNum) {
        return super.getRawValue(ANGLE_AND_TEMPERATURE_NUM - TEMPERATURE_NUM);
    }

    public Float getValue() {
        return super.getRawValue(ANGLE_AND_TEMPERATURE_NUM - TEMPERATURE_NUM);
    }
}
