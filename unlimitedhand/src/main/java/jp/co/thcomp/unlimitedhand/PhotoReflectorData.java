package jp.co.thcomp.unlimitedhand;

public class PhotoReflectorData extends AbstractSensorIntegerData {
    public static final int PHOTO_REFLECTOR_NUM = 8;

    @Override
    public int getSensorNum() {
        return PHOTO_REFLECTOR_NUM;
    }
}
