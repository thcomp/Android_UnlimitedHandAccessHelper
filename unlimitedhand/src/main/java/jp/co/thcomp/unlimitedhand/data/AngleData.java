package jp.co.thcomp.unlimitedhand.data;

public class AngleData extends AbstractSensorIntegerData {
    public static final int ANGLE_NUM = 3;

    @Override
    public int getSensorNum() {
        return ANGLE_NUM;
    }

    @Override
    public Integer getRawValue(int channelNum) {
        Integer ret = super.getRawValue(channelNum);

        if (ret != null) {
            if ((-180 > ret) || (ret > 180)) {
                // invalid value, return 0
                ret = 0;
            }
        }

        return ret;
    }
}
