package jp.co.thcomp.unlimitedhand.data;

import java.util.Arrays;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.LogUtil;

public abstract class AbstractSensorData<DataType> {
    protected final String channelData[] = new String[getSensorNum()];

    abstract public int getSensorNum();

    abstract protected DataType changeDataType(String orgData);

    abstract public boolean isSupportCalibration();

    protected String getRawDataSeparator() {
        return "\\+";
    }

    public AbstractSensorData() {
    }

    public AbstractSensorData(AbstractSensorData srcSensorData) {
        if (srcSensorData == null) {
            throw new NullPointerException("srcSensorData == null");
        }

        if (this.getClass() != srcSensorData.getClass()) {
            throw new IllegalArgumentException("not match class");
        }

        for (int i = 0, size = channelData.length; i < size; i++) {
            channelData[i] = new String(srcSensorData.channelData[i]);
        }
    }

    public boolean expandRawData(byte[] rawData) {
        if (UhAccessHelper.isEnableDebug()) {
            LogUtil.d(UhAccessHelper.TAG, getClass().getSimpleName() + ".expandRawData: rawData = " + Arrays.toString(rawData));
        }

        boolean ret = true;
        String[] rawDataSplitArray = new String(rawData).split(getRawDataSeparator());

        for (int i = 0, size = getSensorNum(); i < size; i++) {
            try {
                channelData[i] = rawDataSplitArray[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                channelData[i] = null;
            } catch (Exception e) {
                ret = false;
                break;
            }
        }

        return ret;
    }

    public DataType getRawValue(int channelNum) {
        DataType ret = null;

        if (channelNum >= 0 && channelNum < getSensorNum()) {
            ret = changeDataType(channelData[channelNum]);
        }

        return ret;
    }

    @Override
    public String toString() {
        return "AbstractSensorData{" +
                "channelData=" + Arrays.toString(channelData) +
                '}';
    }
}
