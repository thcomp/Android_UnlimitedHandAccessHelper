package jp.co.thcomp.unlimitedhand;

abstract class AbstractSensorIntegerData extends AbstractSensorData<Integer> {
    @Override
    protected Integer changeDataType(String orgData) {
        int ret = 0;

        try {
            ret = Integer.valueOf(orgData);
        } catch (NumberFormatException e) {
        }

        return ret;
    }
}
