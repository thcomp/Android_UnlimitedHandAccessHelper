package jp.co.thcomp.unlimited_hand.data;

import android.database.Cursor;

abstract public class AbstractMLSensorData {
    public String label;

    abstract public String getMLSensorData();
    abstract public String getMLSensorData(Cursor cursor);
}
