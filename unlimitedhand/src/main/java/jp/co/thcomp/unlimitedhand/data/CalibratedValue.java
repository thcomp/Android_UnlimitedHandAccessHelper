package jp.co.thcomp.unlimitedhand.data;

public interface CalibratedValue<DataType> {
    public void setCalibrateBaseData(DataType[] baseDataArray);
    public DataType getCalibratedValue(int channelNum);
}
