package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.data.AngleData;

public class CalibratedAngleData extends AngleData implements CalibratedValue<Integer> {
    protected Integer[] mFlatAngleArray = null;

    @Override
    public void setCalibrateBaseData(Integer[] baseDataArray) {
        mFlatAngleArray = baseDataArray;
    }

    @Override
    public Integer getCalibratedValue(int channelNum) {
        int adjustValue = 0;

        try {
            adjustValue = mFlatAngleArray != null ? mFlatAngleArray[channelNum] : 0;
        } catch (Exception e) {
        }

        return getValue(channelNum) - adjustValue;
    }
}
