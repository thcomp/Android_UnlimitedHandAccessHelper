package jp.co.thcomp.unlimitedhand;

public class TemperatureData extends AbstractSensorFloatData {
    public static final int TEMPERATURE_NUM = 1;
    private static final int ANGLE_AND_TEMPERATURE_NUM = 4;

    @Override
    public int getSensorNum() {
        return ANGLE_AND_TEMPERATURE_NUM;
    }

    @Override
    public Float getValue(int channelNum) {
        return super.getValue(ANGLE_AND_TEMPERATURE_NUM - TEMPERATURE_NUM);
    }

    public Float getValue() {
        return super.getValue(ANGLE_AND_TEMPERATURE_NUM - TEMPERATURE_NUM);
    }
}
