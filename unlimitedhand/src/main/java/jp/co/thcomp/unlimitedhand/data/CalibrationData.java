package jp.co.thcomp.unlimitedhand.data;

import java.util.Arrays;

public class CalibrationData {
    public int mAngleFlatAve = 0;
    public Integer[] mPRAveArray = new Integer[PhotoReflectorData.PHOTO_REFLECTOR_NUM];
    public Float[] mAccelTempGyroAveArray = new Float[AccelerationData.ACCELERATION_GYRO_NUM];

    public CalibrationData() {
        jp.co.thcomp.util.LogUtil.printStackTrace();
    }

    @Override
    public String toString() {
        return "CalibrationData{" +
                "mAccelTempGyroAveArray=" + Arrays.toString(mAccelTempGyroAveArray) +
                ", mAngleFlatAve=" + mAngleFlatAve +
                ", mPRAveArray=" + Arrays.toString(mPRAveArray) +
                '}';
    }
}

