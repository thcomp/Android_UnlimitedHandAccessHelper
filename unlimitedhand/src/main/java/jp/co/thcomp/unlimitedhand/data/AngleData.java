package jp.co.thcomp.unlimitedhand.data;

public class AngleData extends AbstractSensorIntegerData {
    public static final int ANGLE_NUM = 3;

    @Override
    public int getSensorNum() {
        return ANGLE_NUM;
    }
}