package jp.co.thcomp.unlimitedhand.data;

public class QuaternionData extends AbstractSensorFloatData {
    public static final int QUATERNION_NUM = 4;

    @Override
    public int getSensorNum() {
        return QUATERNION_NUM;
    }
}
