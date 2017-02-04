package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.data.GyroData;

public class CalibratedGyroData extends GyroData {
    public Float getCalibratedValue(int channelNum) {
        return getValue(channelNum);
    }
}
