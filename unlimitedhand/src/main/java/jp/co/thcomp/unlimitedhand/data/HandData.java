package jp.co.thcomp.unlimitedhand.data;

import jp.co.thcomp.unlimitedhand.UhGestureDetector;

public class HandData {
    public UhGestureDetector.FingerCondition thumb;
    public UhGestureDetector.FingerCondition index;
    public UhGestureDetector.FingerCondition middle;
    public UhGestureDetector.FingerCondition ring;
    public UhGestureDetector.FingerCondition pinky;

    public HandData(UhGestureDetector.FingerCondition baseCondition) {
        thumb = index = middle = ring = pinky = baseCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HandData handData = (HandData) o;

        if (thumb != handData.thumb) return false;
        if (index != handData.index) return false;
        if (middle != handData.middle) return false;
        if (ring != handData.ring) return false;
        return pinky == handData.pinky;
    }

    @Override
    public int hashCode() {
        int result = thumb != null ? thumb.hashCode() : 0;
        result = 31 * result + (index != null ? index.hashCode() : 0);
        result = 31 * result + (middle != null ? middle.hashCode() : 0);
        result = 31 * result + (ring != null ? ring.hashCode() : 0);
        result = 31 * result + (pinky != null ? pinky.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HandData{" +
                "thumb=" + (thumb != null ? thumb.name() : "null") +
                ",index=" + (index != null ? index.name() : "null") +
                ", middle=" + (middle != null ? middle.name() : "null") +
                ", ring=" + (ring != null ? ring.name() : "null") +
                ", pinky=" + (pinky != null ? pinky.name() : "null") +
                '}';
    }
}
