package jp.co.thcomp.unlimited_hand.data;

import android.database.Cursor;

import java.lang.reflect.Field;

import jp.co.thcomp.unlimited_hand.MLSensorValueDatabase;
import jp.co.thcomp.unlimitedhand.UhGestureDetector2;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.unlimitedhand.data.QuaternionData;
import jp.co.thcomp.unlimitedhand.data.TemperatureData;

public class MLSensorData extends AbstractMLSensorData {
    private static final int[] DEFAULT_DATA_ARRAY_SIZE = {
            AccelerationData.ACCELERATION_NUM,
            GyroData.GYRO_NUM,
            PhotoReflectorData.PHOTO_REFLECTOR_NUM,
            AngleData.ANGLE_NUM,
            TemperatureData.TEMPERATURE_NUM,
            QuaternionData.QUATERNION_NUM,
            1,  // ambient light size
    };

    public String[] accelDataArray = null;
    public String[] gyroDataArray = null;
    public String[] prDataArray = null;
    public String[] angleDataArray = null;
    public String[] temperatureDataArray = null;
    public String[] quaternionDataArray = null;
    public String[] ambientLightDataArray = null;
    private String[][] mDataArrays = null;

    public MLSensorData() {
        accelDataArray = new String[AccelerationData.ACCELERATION_NUM];
        gyroDataArray = new String[GyroData.GYRO_NUM];
        prDataArray = new String[PhotoReflectorData.PHOTO_REFLECTOR_NUM];
        angleDataArray = new String[AngleData.ANGLE_NUM];
        temperatureDataArray = new String[TemperatureData.TEMPERATURE_NUM];
        quaternionDataArray = new String[QuaternionData.QUATERNION_NUM];
        ambientLightDataArray = new String[1];

        mDataArrays = new String[][]{
                accelDataArray,
                gyroDataArray,
                prDataArray,
                angleDataArray,
                temperatureDataArray,
                quaternionDataArray,
                ambientLightDataArray,
        };
    }

    public MLSensorData(boolean... useData) {
        if (useData != null) {
            int index = UhGestureDetector2.USE_SENSOR_INDEX_ACCELERATION;
            if ((useData.length > index) && useData[index]) {
                accelDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            index = UhGestureDetector2.USE_SENSOR_INDEX_GYRO;
            if ((useData.length > index) && useData[index]) {
                gyroDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            index = UhGestureDetector2.USE_SENSOR_INDEX_PHOTO_REFLECTOR;
            if ((useData.length > index) && useData[index]) {
                prDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            index = UhGestureDetector2.USE_SENSOR_INDEX_ANGLE;
            if ((useData.length > index) && useData[index]) {
                angleDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            index = UhGestureDetector2.USE_SENSOR_INDEX_TEMPERATURE;
            if ((useData.length > index) && useData[index]) {
                temperatureDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            index = UhGestureDetector2.USE_SENSOR_INDEX_QUATERNION;
            if ((useData.length > index) && useData[index]) {
                quaternionDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            index = UhGestureDetector2.USE_SENSOR_INDEX_AMBIENT_LIGHT;
            if ((useData.length > index) && useData[index]) {
                ambientLightDataArray = new String[DEFAULT_DATA_ARRAY_SIZE[index]];
            }

            mDataArrays = new String[][]{
                    accelDataArray,
                    gyroDataArray,
                    prDataArray,
                    angleDataArray,
                    temperatureDataArray,
                    quaternionDataArray,
                    ambientLightDataArray,
            };
        } else {
            accelDataArray = new String[AccelerationData.ACCELERATION_NUM];
            gyroDataArray = new String[GyroData.GYRO_NUM];
            prDataArray = new String[PhotoReflectorData.PHOTO_REFLECTOR_NUM];
            angleDataArray = new String[AngleData.ANGLE_NUM];
            temperatureDataArray = new String[TemperatureData.TEMPERATURE_NUM];
            quaternionDataArray = new String[QuaternionData.QUATERNION_NUM];
            ambientLightDataArray = new String[1];

            mDataArrays = new String[][]{
                    accelDataArray,
                    gyroDataArray,
                    prDataArray,
                    angleDataArray,
                    temperatureDataArray,
                    quaternionDataArray,
                    ambientLightDataArray,
            };
        }
    }

    @Override
    public String getMLSensorData() {
        // Accel0_Accel1_Accel2+Gyro0_Gyro1_Gyro3+PR0_PR1_PR2_PR3_PR4_PR5_PR6_PR7+Angle0_Angle1_Angle2+Temp+Quater0_Quater1_Quater2_Quater3+AmbientLight,Data
        StringBuilder builder = new StringBuilder();

        for (int i = 0, sizeI = mDataArrays.length; i < sizeI; i++) {
            if (i != 0) {
                builder.append("+");
            }

            if (mDataArrays[i] != null) {
                for (int j = 0, sizeJ = mDataArrays[i].length; j < sizeJ; j++) {
                    if (j != 0) {
                        builder.append("_");
                    }
                    builder.append(mDataArrays[i][j]);
                }
            } else {
                for (int j = 0, sizeJ = DEFAULT_DATA_ARRAY_SIZE[i]; j < sizeJ; j++) {
                    if (j != 0) {
                        builder.append("_");
                    }
                    builder.append("null");
                }
            }
        }
        builder.append(",").append(label);

        return builder.toString();
    }

    @Override
    public String getMLSensorData(Cursor cursor) {
        // Accel0_Accel1_Accel2+Gyro0_Gyro1_Gyro3+PR0_PR1_PR2_PR3_PR4_PR5_PR6_PR7+Angle0_Angle1_Angle2+Temp+Quater0_Quater1_Quater2_Quater3+AmbientLight,Data
        StringBuilder builder = new StringBuilder();
        Field[] dataArrayFields = getClass().getFields();

        for (int i = 0, sizeI = mDataArrays.length; i < sizeI; i++) {
            Field targetDataField = null;
            for (int j = 0, sizeJ = dataArrayFields.length; j < sizeJ; j++) {
                if (mDataArrays[i] != null) {
                    try {
                        if (mDataArrays[i].equals(dataArrayFields[j].get(this))) {
                            targetDataField = dataArrayFields[j];
                            break;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (i != 0) {
                builder.append("+");
            }

            if (targetDataField != null) {
                for (int j = 0, sizeJ = mDataArrays[i].length; j < sizeJ; j++) {
                    if (j != 0) {
                        builder.append("_");
                    }
                    String columnName = MLSensorValueDatabase.getSensorValueColumnName(targetDataField.getName(), j);
                    builder.append(cursor.getString(cursor.getColumnIndex(columnName)));
                }
            } else {
                for (int j = 0, sizeJ = DEFAULT_DATA_ARRAY_SIZE[i]; j < sizeJ; j++) {
                    if (j != 0) {
                        builder.append("_");
                    }
                    builder.append("null");
                }
            }
        }
        builder.append(",").append(cursor.getString(cursor.getColumnIndex("label")));

        return builder.toString();
    }
}
