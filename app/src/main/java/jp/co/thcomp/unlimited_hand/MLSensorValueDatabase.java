package jp.co.thcomp.unlimited_hand;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import jp.co.thcomp.unlimited_hand.data.AbstractMLSensorData;
import jp.co.thcomp.unlimited_hand.data.MLSensorData;
import jp.co.thcomp.util.LogUtil;

public class MLSensorValueDatabase extends SQLiteOpenHelper {
    private static final String TAG = MLSensorValueDatabase.class.getSimpleName();
    private static final String DATABASE_NAME = "ml_sensor_value.db";
    public static final String COLUMN_CDATE = "cdate";
    public static final String COLUMN_LABEL = "label";

    public static final String getSensorValueColumnName(String fieldName, int index) {
        return String.format("%1$s_%2$02d", fieldName, index);
    }

    private static final Class[] BASE_DATA_CLASS_ARRAY = {
            MLSensorData.class,
    };

    public MLSensorValueDatabase(Context context, int version) {
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
        queryBuilder.append(COLUMN_CDATE).append(" integer primary key");

        try {
            Constructor constructor = baseDataClass.getConstructor();
            Object instance = constructor.newInstance();

            Field[] sensorDataArrayFields = baseDataClass.getFields();
            for (Field sensorDataArrayField : sensorDataArrayFields) {
                try {
                    if (sensorDataArrayField.getType() == String[].class) {
                        String[] dataArray = (String[]) sensorDataArrayField.get(instance);
                        if (dataArray != null) {
                            for (int i = 0, size = dataArray.length; i < size; i++) {
                                queryBuilder.append(String.format(", %s text", getSensorValueColumnName(sensorDataArrayField.getName(), i)));
                            }
                        }
                    } else if (sensorDataArrayField.getType() == String.class) {
                        queryBuilder.append(String.format(", %s text", sensorDataArrayField.getName()));
                    }
                } catch (Exception e) {
                    LogUtil.exception(TAG, e);
                }
            }
        } catch (Exception e) {
            LogUtil.exception(TAG, e);
        }

        queryBuilder.append(");");

        sqLiteDatabase.execSQL(queryBuilder.toString());
    }

    public boolean insertData(AbstractMLSensorData data) {
        String tableName = data.getClass().getSimpleName();
        ContentValues values = new ContentValues();

        values.put(COLUMN_CDATE, System.currentTimeMillis());
        Field[] sensorDataArrayFields = data.getClass().getFields();
        for (Field sensorDataArrayField : sensorDataArrayFields) {
            try {
                if (sensorDataArrayField.getType() == String[].class) {
                    String[] dataArray = (String[]) sensorDataArrayField.get(data);
                    for (int i = 0, size = dataArray.length; i < size; i++) {
                        values.put(getSensorValueColumnName(sensorDataArrayField.getName(), i), dataArray[i]);
                    }
                } else if (sensorDataArrayField.getType() == String.class) {
                    values.put(sensorDataArrayField.getName(), (String) sensorDataArrayField.get(data));
                }
            } catch (Exception e) {
                LogUtil.exception(TAG, e);
            }
        }

        return getWritableDatabase().insert(tableName, null, values) == 1;
    }

    public int clearData() {
        SQLiteDatabase database = getWritableDatabase();
        int clearDataCount = 0;

        database.beginTransaction();
        for (Class targetClass : BASE_DATA_CLASS_ARRAY) {
            clearDataCount += database.delete(targetClass.getSimpleName(), null, null);
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
