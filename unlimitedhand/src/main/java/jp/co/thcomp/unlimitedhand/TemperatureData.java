package jp.co.thcomp.unlimitedhand;

public class TemperatureData extends AbstractSensorFloatData {
    public static final int ANGLE_AND_TEMPERATURE_NUM = 4;

    @Override
    protected int getSensorNum() {
        return ANGLE_AND_TEMPERATURE_NUM;
    }

    @Override
    public Float getValue(int channelNum) {
        return super.getValue(ANGLE_AND_TEMPERATURE_NUM - 1);
    }

    public Float getValue() {
        return super.getValue(ANGLE_AND_TEMPERATURE_NUM - 1);
    }
}
