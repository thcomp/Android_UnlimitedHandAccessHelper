package jp.co.thcomp.unlimitedhand.data;

public class AngleData extends AbstractSensorIntegerData {
    public static final int ANGLE_NUM = 3;

    @Override
    public int getSensorNum() {
        return ANGLE_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return true;
    }

    @Override
    public boolean calibrate(CalibrationData calibrationData) {
        boolean ret = super.calibrate(calibrationData);

        if (ret) {
            for (int i = 0, size = getSensorNum(); i < size; i++) {
                calibratedChannelData[i] = String.valueOf(getRawValue(i) - calibrationData.mAngleFlatAve);
            }
        }

        return ret;
    }
}
