package jp.co.thcomp.unlimitedhand;

public class CalibrationCondition {
    public enum HandStatus {
        HandOpen,
        HandClose,
        PickObject,
        ;
    }

    public int deviceAngleDegree;
    public HandStatus handStatus;

    public CalibrationCondition(int deviceAngleDegree, HandStatus handStatus){
        this.deviceAngleDegree = deviceAngleDegree;
        this.handStatus = handStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalibrationCondition that = (CalibrationCondition) o;

        if (deviceAngleDegree != that.deviceAngleDegree) return false;
        return handStatus == that.handStatus;

    }

    @Override
    public int hashCode() {
        int result = deviceAngleDegree;
        result = 31 * result + (handStatus != null ? handStatus.hashCode() : 0);
        return result;
    }
}

