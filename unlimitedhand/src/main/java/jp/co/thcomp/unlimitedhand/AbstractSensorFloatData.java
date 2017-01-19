package jp.co.thcomp.unlimitedhand;

abstract class AbstractSensorFloatData extends AbstractSensorData<Float> {
    @Override
    protected Float changeDataType(String orgData) {
        float ret = 0f;

        try {
            ret = Float.valueOf(orgData);
        } catch (NumberFormatException e) {
        }

        return ret;
    }
}
