package jp.co.thcomp.unlimitedhand.data;

import java.util.Arrays;

public class CalibrationData {
    public int mAngleFlatAve = 0;
    public Integer[] mPRAveArray = new Integer[PhotoReflectorData.PHOTO_REFLECTOR_NUM];

    @Override
    public String toString() {
        return "CalibrationData{" +
                "mAngleFlatAve=" + mAngleFlatAve +
                ", mPRAveArray=" + Arrays.toString(mPRAveArray) +
                '}';
    }
}

