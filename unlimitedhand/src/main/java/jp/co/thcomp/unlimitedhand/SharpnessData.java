package jp.co.thcomp.unlimitedhand;

public class SharpnessData extends AbstractSensorIntegerData {
    public static final int SHARPNESS_NUM = 1;

    @Override
    public int getSensorNum() {
        return SHARPNESS_NUM;
    }

    /**
     * expand "stimulationTime: XX" or "Current stimulation time is MINIMAM: 200 mSec(0.2sec)"
     * XX: stimulation time
     *
     * @param rawData
     * @return
     */
    @Override
    public boolean expandRawData(byte[] rawData) {
        boolean ret = true;
        String[] rawDataSplitArray = new String(rawData).split(":");

        if(rawDataSplitArray.length > 0){
            if(rawDataSplitArray[0].equals("stimulationTime")){
                try {
                    channelData[0] = rawDataSplitArray[1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    channelData[0] = null;
                } catch (Exception e) {
                    ret = false;
                }
            }else if(rawDataSplitArray[0].equals("Current stimulation time is MINIMAM")){
                rawDataSplitArray[1] = rawDataSplitArray[1].trim();
                rawDataSplitArray = rawDataSplitArray[1].split(" ");

                if(rawDataSplitArray.length > 0){
                    channelData[0] = rawDataSplitArray[0];
                }
            }
        }

        return ret;
    }
}
