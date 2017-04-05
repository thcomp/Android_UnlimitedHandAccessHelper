package jp.co.thcomp.unlimited_hand.data;

import android.database.Cursor;

import java.lang.reflect.Field;

import jp.co.thcomp.unlimited_hand.MLSensorValueDatabase;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;

public class MLSensorData extends AbstractMLSensorData {
    public String[] accelDataArray = new String[AccelerationData.ACCELERATION_NUM];
    public String[] gyroDataArray = new String[GyroData.GYRO_NUM];
    public String[] prDataArray = new String[PhotoReflectorData.PHOTO_REFLECTOR_NUM];
    private String[][] mDataArrays = {
            accelDataArray,
            gyroDataArray,
            prDataArray,
    };

    @Override
    public String getMLSensorData() {
        // Accel0_Accel1_Accel2+Gyro0_Gyro1_Gyro3+PR0_PR1_PR2_PR3_PR4_PR5_PR6_PR7,Data
        StringBuilder builder = new StringBuilder();

        for (int i = 0, sizeI = mDataArrays.length; i < sizeI; i++) {
            if (i != 0) {
                builder.append("+");
            }

            for (int j = 0, sizeJ = mDataArrays[i].length; j < sizeJ; j++) {
                if (j != 0) {
                    builder.append("_");
                }
                builder.append(mDataArrays[i][j]);
            }
        }
        builder.append(",").append(label);

        return builder.toString();
    }

    @Override
    public String getMLSensorData(Cursor cursor) {
        // Accel0_Accel1_Accel2+Gyro0_Gyro1_Gyro3+PR0_PR1_PR2_PR3_PR4_PR5_PR6_PR7,Data
        StringBuilder builder = new StringBuilder();
        Field[] dataArrayFields = getClass().getFields();

        for (int i = 0, sizeI = mDataArrays.length; i < sizeI; i++) {
            Field targetDataField = null;
            for (int j = 0, sizeJ = dataArrayFields.length; j < sizeJ; j++) {
                try {
                    if (mDataArrays[i].equals(dataArrayFields[i].get(this))) {
                        targetDataField = dataArrayFields[i];
                        break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            if (targetDataField != null) {
                if (i != 0) {
                    builder.append("+");
                }

                for (int j = 0, sizeJ = mDataArrays[i].length; j < sizeJ; j++) {
                    if (j != 0) {
                        builder.append("_");
                    }
                    String columnName = MLSensorValueDatabase.getSensorValueColumnName(targetDataField.getName(), j);
                    builder.append(cursor.getString(cursor.getColumnIndex(columnName)));
                }
            }
        }
        builder.append(",").append(cursor.getString(cursor.getColumnIndex("label")));

        return builder.toString();
    }
}
