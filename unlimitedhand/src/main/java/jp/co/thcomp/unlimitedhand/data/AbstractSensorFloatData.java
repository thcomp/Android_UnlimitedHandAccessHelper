package jp.co.thcomp.unlimitedhand.data;

public abstract class AbstractSensorFloatData extends AbstractSensorData<Float> {
    public AbstractSensorFloatData() {
        super();
    }

    public AbstractSensorFloatData(AbstractSensorData srcSensorData) {
        super(srcSensorData);
    }

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
