package jp.co.thcomp.unlimitedhand;

public abstract class AbstractSensorData<DataType> {
    protected final String channelData[] = new String[getSensorNum()];

    abstract public int getSensorNum();
    abstract protected DataType changeDataType(String orgData);

    public boolean expandRawData(byte[] rawData) {
        boolean ret = true;
        String[] rawDataSplitArray = new String(rawData).split("_");

        for (int i = 0, size=getSensorNum(); i < size; i++) {
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

    public DataType getValue(int channelNum) {
        DataType ret = null;

        if (channelNum >= 0 && channelNum < getSensorNum()) {
            ret = changeDataType(channelData[channelNum]);
        }

        return ret;
    }
}
