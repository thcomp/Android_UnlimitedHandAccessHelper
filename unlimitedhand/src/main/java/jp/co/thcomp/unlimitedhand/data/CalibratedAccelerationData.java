package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.data.AccelerationData;

public class CalibratedAccelerationData extends AccelerationData implements CalibratedValue<Float> {
    @Override
    public void setCalibrateBaseData(Float[] baseDataArray) {
        // 処理なし
    }

    @Override
    public Float getCalibratedValue(int channelNum) {
        return getValue(channelNum);
    }
}
