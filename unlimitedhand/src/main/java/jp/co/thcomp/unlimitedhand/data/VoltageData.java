package jp.co.thcomp.unlimitedhand.data;

public class VoltageData extends AbstractSensorIntegerData {
    public static final boolean IS_SUPPORT_CALIBRATION = false;
    public static final int VOLTAGE_NUM = 1;

    @Override
    public int getSensorNum() {
        return VOLTAGE_NUM;
    }

    @Override
    public boolean isSupportCalibration() {
        return IS_SUPPORT_CALIBRATION;
    }

    /**
     * expand "Vol:XX" or "Vol:XX, it is maximum of the EMS Voltage"
     * XX: voltage value
     *
     * @param rawData
     * @return
     */
    @Override
    public boolean expandRawData(byte[] rawData) {
        boolean ret = true;

        if (rawData != null) {
            String[] rawDataSplitArray = new String(rawData).split(",");

            if (rawDataSplitArray.length > 0) {
                rawDataSplitArray = rawDataSplitArray[0].split(":");
                if (rawDataSplitArray.length > 1) {
                    try {
                        channelData[0] = rawDataSplitArray[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        channelData[0] = null;
                    } catch (Exception e) {
                        ret = false;
                    }
                }
            }
        } else {
            ret = false;
        }

        return ret;
    }


}
