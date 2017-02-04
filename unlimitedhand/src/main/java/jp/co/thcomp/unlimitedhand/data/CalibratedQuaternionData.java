package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.data.QuaternionData;

public class CalibratedQuaternionData extends QuaternionData implements CalibratedValue<Integer> {
    @Override
    public void setCalibrateBaseData(Integer[] baseDataArray) {
        // 処理なし
    }

    @Override
    public Integer getCalibratedValue(int channelNum) {
        return getValue(channelNum);
    }
}
