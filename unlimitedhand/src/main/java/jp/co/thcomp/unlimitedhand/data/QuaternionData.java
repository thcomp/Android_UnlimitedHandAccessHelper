package jp.co.thcomp.unlimitedhand.data;

public class QuaternionData extends AbstractSensorFloatData {
    public static final boolean IS_SUPPORT_CALIBRATION = false;
    public static final int QUATERNION_NUM = 4;

    public QuaternionData() {
        super();
    }

    public QuaternionData(AbstractSensorData srcSensorData) {
        super(srcSensorData);
    }

    @Override
    public int getSensorNum() {
        return QUATERNION_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }
}
