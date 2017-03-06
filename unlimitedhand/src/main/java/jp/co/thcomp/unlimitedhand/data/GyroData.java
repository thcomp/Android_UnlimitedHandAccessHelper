package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.LogUtil;

public class GyroData extends AbstractSensorFloatData {
    public static final boolean IS_SUPPORT_CALIBRATION = true;
    public static final int GYRO_NUM = 3;
    public static final int SEPARATE_DATA_NUM = 1;
    public static final int ACCELERATION_GYRO_NUM = AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + GyroData.GYRO_NUM;

    @Override
    public int getSensorNum() {
        return ACCELERATION_GYRO_NUM;
    }

    @Override
    public Float getRawValue(int channelNum) {
        float ret = 0;

        if (channelNum >= 0 && channelNum < GYRO_NUM) {
            ret = super.getRawValue(AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + channelNum);
        }

        return ret;
    }

    @Override
    public Float getCalibratedValue(int channelNum) {
        float ret = 0;

        if (channelNum >= 0 && channelNum < GYRO_NUM) {
            ret = super.getCalibratedValue(AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM + channelNum);
        }

        return ret;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }

    @Override
    public boolean calibrate(CalibrationData calibrationData) {
        boolean ret = super.calibrate(calibrationData);

        if (ret) {
            int baseIndex = AccelerationData.ACCELERATION_NUM + SEPARATE_DATA_NUM;
            for (int i = baseIndex, size = getSensorNum(); i < size; i++) {
                if (UhAccessHelper.isEnableDebug()) {
                    LogUtil.d(UhAccessHelper.TAG,
                            GyroData.class.getSimpleName() +
                                    ": value=" + String.valueOf(getRawValue(i - baseIndex) - calibrationData.mAccelGyroAveArray[i]) +
                                    ", raw value(" + (i - baseIndex) + ")=" + getRawValue(i - baseIndex) + ", ave=" + calibrationData.mAccelGyroAveArray[i]);
                }
                calibratedChannelData[i] = String.valueOf(getRawValue(i - baseIndex) - calibrationData.mAccelGyroAveArray[i]);
            }
        }

        return ret;
    }
}
