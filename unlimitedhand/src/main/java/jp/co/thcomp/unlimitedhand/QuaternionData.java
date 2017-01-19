package jp.co.thcomp.unlimitedhand;

public class QuaternionData extends AbstractSensorIntegerData {
    public static final int QUATERNION_NUM = 4;

    @Override
    protected int getSensorNum() {
        return QUATERNION_NUM;
    }
}
