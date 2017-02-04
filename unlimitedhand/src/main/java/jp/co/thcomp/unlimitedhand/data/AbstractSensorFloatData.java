package jp.co.thcomp.unlimitedhand.data;

public abstract class AbstractSensorFloatData extends AbstractSensorData<Float> {
    @Override
    protected Float changeDataType(String orgData) {
        float ret = 0f;

        try {
            ret = Float.valueOf(orgData);
        } catch (NumberFormatException e) {
        } catch (Exception e) {
        }

        return ret;
    }
}
