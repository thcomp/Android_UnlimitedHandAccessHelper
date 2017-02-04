package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;

public class CalibratedPhotoReflectorData extends PhotoReflectorData implements CalibratedValue<Integer> {
    protected Integer[] mPhotoReflectorAveArray = null;

    @Override
    public void setCalibrateBaseData(Integer[] baseDataArray) {
        mPhotoReflectorAveArray = baseDataArray;

    }

    @Override
    public Integer getCalibratedValue(int channelNum) {
        int adjustValue = 0;

        try {
            adjustValue = mPhotoReflectorAveArray != null ? mPhotoReflectorAveArray[channelNum] : 0;
        } catch (Exception e) {
        }

        return getValue(channelNum) - adjustValue;
    }
}
