package jp.co.thcomp.unlimitedhand;

public class PhotoSensorData extends AbstractSensorIntegerData {
    public static final int PHOTO_SENSOR_NUM = 8;

    @Override
    protected int getSensorNum() {
        return PHOTO_SENSOR_NUM;
    }
}
