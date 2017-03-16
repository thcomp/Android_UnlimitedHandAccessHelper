package jp.co.thcomp.unlimitedhand.data;

public abstract class AbstractSensorIntegerData extends AbstractSensorData<Integer> {
    public AbstractSensorIntegerData() {
        super();
    }

    public AbstractSensorIntegerData(AbstractSensorData srcSensorData) {
        super(srcSensorData);
    }

    @Override
    protected Integer changeDataType(String orgData) {
        int ret = 0;

        try {
            ret = Integer.valueOf(orgData);
        } catch (NumberFormatException e) {
        } catch (Exception e) {
        }

        return ret;
    }
}
