package jp.co.thcomp.unlimited_hand;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.unlimitedhand.data.QuaternionData;
import jp.co.thcomp.unlimitedhand.data.TemperatureData;

public class SensorValueDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sensor_value.db";
    public static final String COLUMN_CDATE = "cdate";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_SENSOR_VALUE_PREFIX = "value_";
    public static final String COLUMN_CALIBRATED_SENSOR_VALUE_PREFIX = "calibrated_value_";

    public static final String getSensorValueColumnName(int index) {
        return String.format("%1$s%2$02d", COLUMN_SENSOR_VALUE_PREFIX, index);
    }

    public static final String getCalibratedSensorValueColumnName(int index) {
        return String.format("%1$s%2$02d", COLUMN_CALIBRATED_SENSOR_VALUE_PREFIX, index);
    }

    private static final Class[] BASE_DATA_CLASS_ARRAY = {
            PhotoReflectorData.class,
            AngleData.class,
            TemperatureData.class,
            AccelerationData.class,
            GyroData.class,
            QuaternionData.class,
    };

    public SensorValueDatabase(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.beginTransaction();
        for (int i = 0, size = BASE_DATA_CLASS_ARRAY.length; i < size; i++) {
            createTable(sqLiteDatabase, BASE_DATA_CLASS_ARRAY[i]);
        }
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.beginTransaction();
        for (int i = 0, size = BASE_DATA_CLASS_ARRAY.length; i < size; i++) {
            createTable(sqLiteDatabase, BASE_DATA_CLASS_ARRAY[i]);
        }
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }

    private void createTable(SQLiteDatabase sqLiteDatabase, Class baseDataClass) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("create table if not exists ").append(baseDataClass.getSimpleName()).append("(");

        // default data
        queryBuilder.append(COLUMN_CDATE).append(" integer primary key, ").append(COLUMN_DESCRIPTION).append(" text");

        boolean isSupportCalibration = false;
        Integer dataSensorNum = null;
        Method getSensorNumMethod = null;
        Method isSupportCalibrationMethod = null;
        Constructor constructor = null;
        Object instance = null;
        try {
            constructor = baseDataClass.getConstructor();
            instance = constructor.newInstance();

            getSensorNumMethod = baseDataClass.getMethod("getSensorNum");
            isSupportCalibrationMethod = baseDataClass.getMethod("isSupportCalibration");
            dataSensorNum = (int) getSensorNumMethod.invoke(instance);
            isSupportCalibration = (boolean) isSupportCalibrationMethod.invoke(instance);
        } catch (Exception e) {
        }

        if (dataSensorNum != null) {
            for (int i = 0; i < dataSensorNum; i++) {
                queryBuilder.append(String.format(", %s real", getSensorValueColumnName(i)));
            }

            if (isSupportCalibration) {
                for (int i = 0; i < dataSensorNum; i++) {
                    queryBuilder.append(String.format(", %s real", getCalibratedSensorValueColumnName(i)));
                }
            }
        }

        queryBuilder.append(");");

        sqLiteDatabase.execSQL(queryBuilder.toString());
    }

    public boolean insertData(AbstractSensorData data) {
        String tableName = data.getClass().getSimpleName();
        ContentValues values = new ContentValues();

        values.put(COLUMN_CDATE, System.currentTimeMillis());
        for (int i = 0, size = data.getSensorNum(); i < size; i++) {
            Object value = data.getRawValue(i);
            if (value != null) {
                if (value.getClass() == Integer.class || value.getClass() == int.class) {
                    values.put(getSensorValueColumnName(i), (int) value);
                } else if (value.getClass() == Float.class || value.getClass() == float.class) {
                    values.put(getSensorValueColumnName(i), (float) value);
                }
            }
        }

        if (data.isSupportCalibration()) {
            for (int i = 0, size = data.getSensorNum(); i < size; i++) {
                Object value = data.getCalibratedValue(i);
                if (value != null) {
                    if (value.getClass() == Integer.class || value.getClass() == int.class) {
                        values.put(getSensorValueColumnName(i), (int) value);
                    } else if (value.getClass() == Float.class || value.getClass() == float.class) {
                        values.put(getSensorValueColumnName(i), (float) value);
                    }
                }
            }
        }

        return getWritableDatabase().insert(tableName, null, values) == 1;
    }

    public boolean insertMark(String description) {
        return insertMark(BASE_DATA_CLASS_ARRAY, description);
    }

    public boolean insertMark(Class[] targetTableClassArray, String description) {
        int insertNum = 0;
        int targetCount = 0;
        long currentTimeMS = System.currentTimeMillis();

        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(COLUMN_CDATE, currentTimeMS);
        values.put(COLUMN_DESCRIPTION, description);

        if (targetTableClassArray == null) {
            targetTableClassArray = BASE_DATA_CLASS_ARRAY;
        }

        for (int i = 0, size = targetTableClassArray.length; i < size; i++) {
            if (targetTableClassArray[i] != null) {
                targetCount++;
                String tableName = targetTableClassArray[i].getSimpleName();
                insertNum += database.insert(tableName, null, values);
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        return insertNum == targetCount;
    }

    public int clearData(Class[] targetTableClassArray) {
        SQLiteDatabase database = getWritableDatabase();
        int clearDataCount = 0;

        database.beginTransaction();
        for (int i = 0, size = targetTableClassArray.length; i < size; i++) {
            clearDataCount += database.delete(targetTableClassArray[i].getSimpleName(), null, null);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        return clearDataCount;
    }

    public Cursor getData(Class targetTableClass) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(targetTableClass.getSimpleName(), null, null, null, null, null, COLUMN_CDATE + " asc");

        return cursor;
    }
}
