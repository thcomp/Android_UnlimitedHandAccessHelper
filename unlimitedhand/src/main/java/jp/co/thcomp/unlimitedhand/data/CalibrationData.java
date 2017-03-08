package jp.co.thcomp.unlimitedhand.data;

import java.util.Arrays;

public class CalibrationData {
    public int mAngleFlatAve = 0;
    public Integer[] mPRAveArray = new Integer[PhotoReflectorData.PHOTO_REFLECTOR_NUM];
    public Float[] mAccelGyroAveArray = new Float[AccelerationData.ACCELERATION_GYRO_NUM];
    public Float[] mQuaternionAveArray = new Float[QuaternionData.QUATERNION_NUM];

    public CalibrationData() {
        jp.co.thcomp.util.LogUtil.printStackTrace();
    }

    @Override
    public String toString() {
        return "CalibrationData{" +
                "mAccelGyroAveArray=" + Arrays.toString(mAccelGyroAveArray) +
                ", mAngleFlatAve=" + mAngleFlatAve +
                ", mPRAveArray=" + Arrays.toString(mPRAveArray) +
                ", mQuaternionAveArray=" + Arrays.toString(mQuaternionAveArray) +
                '}';
    }
}

