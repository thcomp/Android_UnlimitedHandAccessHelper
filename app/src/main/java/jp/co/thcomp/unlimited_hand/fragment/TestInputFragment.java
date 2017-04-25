package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

import jp.co.thcomp.unlimited_hand.Common;
import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimited_hand.SensorValueDatabase;
import jp.co.thcomp.unlimitedhand.CalibrationCondition;
import jp.co.thcomp.unlimitedhand.CalibrationStatus;
import jp.co.thcomp.unlimitedhand.OnCalibrationStatusChangeListener;
import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AccelerationData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.unlimitedhand.data.GyroData;
import jp.co.thcomp.unlimitedhand.data.PhotoReflectorData;
import jp.co.thcomp.unlimitedhand.data.QuaternionData;
import jp.co.thcomp.unlimitedhand.data.TemperatureData;
import jp.co.thcomp.util.IntentUtil;
import jp.co.thcomp.util.LogUtil;
import jp.co.thcomp.util.PreferenceUtil;
import jp.co.thcomp.util.ThreadUtil;
import jp.co.thcomp.util.ToastUtil;

public class TestInputFragment extends AbstractTestFragment {
    private static final String TAG = TestInputFragment.class.getSimpleName();
    private static final String TEMPORARY_ZIP_FILE = "sensor_data_%1$04d%2$02d%3$02d_%4$02d%5$02d%6$02d.gzip";
    private static final String PREF_LAST_MAIL_ADDRESS = "PREF_LAST_MAIL_ADDRESS";
    private static final int REQUEST_CODE_WRITE_STORAGE = "REQUEST_CODE_WRITE_STORAGE".hashCode() & 0x0000FFFF;
    private static final int DEFAULT_READ_FPS = 30;
    private static final int DEFAULT_READ_INTERVAL_MS = (int) (1000 / DEFAULT_READ_FPS);
    private static final int DEFAULT_CALIBRATION_ANGLE = 0;

    private enum READ_SENSOR {
        PHOTO(R.id.cbPhotoSensor, PhotoReflectorData.class,
                R.id.tvPhotoSensor0, R.id.tvPhotoSensor1, R.id.tvPhotoSensor2, R.id.tvPhotoSensor3,
                R.id.tvPhotoSensor4, R.id.tvPhotoSensor5, R.id.tvPhotoSensor6, R.id.tvPhotoSensor7),
        ANGLE(R.id.cbAngle, AngleData.class,
                R.id.tvAngle0, R.id.tvAngle1, R.id.tvAngle2),
        TEMPERATURE(R.id.cbTemperature, TemperatureData.class, R.id.tvTemperature0),
        ACCELERATION(R.id.cbAcceleration, AccelerationData.class,
                R.id.tvAcceleration0, R.id.tvAcceleration1, R.id.tvAcceleration2),
        GYRO(R.id.cbGyro, GyroData.class,
                R.id.tvGyro0, R.id.tvGyro1, R.id.tvGyro2),
        QUATERNION(R.id.cbQuaternion, QuaternionData.class,
                R.id.tvQuaternion0, R.id.tvQuaternion1, R.id.tvQuaternion2, R.id.tvQuaternion3),;

        int mViewResId;
        Class mDataClass;
        int[] mDisplayValueResIds;

        READ_SENSOR(int viewResId, Class dataClass, int... displayValueResIds) {
            mViewResId = viewResId;
            mDataClass = dataClass;
            mDisplayValueResIds = displayValueResIds;
        }
    }

    private UhGestureDetector mGestureDetector;
    private SensorValueDatabase mDatabase;
    private ReadInputSensorTask mReadInputSensorTask;
    private EditText mCalibrateDeviceAngle;
    private EditText mMarkDescription;
    private EditText mAddress;
    private PhotoReflectorData mPhotoReflectorData = new PhotoReflectorData();
    private AngleData mAngleData = new AngleData();
    private TemperatureData mTemperatureData = new TemperatureData();
    private AccelerationData mAccelerationData = new AccelerationData();
    private GyroData mGyroData = new GyroData();
    private QuaternionData mQuaternionData = new QuaternionData();
    private TextView[][] mTvReadSensorValues = {
            new TextView[PhotoReflectorData.PHOTO_REFLECTOR_NUM],
            new TextView[AngleData.ANGLE_NUM],
            new TextView[TemperatureData.TEMPERATURE_NUM],
            new TextView[AccelerationData.ACCELERATION_NUM],
            new TextView[GyroData.GYRO_NUM],
            new TextView[QuaternionData.QUATERNION_NUM],
    };
    private TextView[] mTvRotateAxisAndDegreeValues = new TextView[4];
    private SaveSensorDataTask mSaveSensorDataTask = null;
    private ClearSensorDataTask mClearSensorDataTask = null;
    private AbstractSensorData[] mSensorDataArray = {
            mPhotoReflectorData,
            mAngleData,
            mTemperatureData,
            mAccelerationData,
            mGyroData,
            mQuaternionData,
    };

    public TestInputFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestInputFragment.
     */
    public static TestInputFragment newInstance() {
        TestInputFragment fragment = new TestInputFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = new SensorValueDatabase(getContext(), 1);
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_input;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = super.onCreateView(inflater, container, savedInstanceState);

        mGestureDetector = new UhGestureDetector(getActivity(), mUHAccessHelper, UhGestureDetector.WearDevice.RightArm);
        mCalibrateDeviceAngle = (EditText) mRootView.findViewById(R.id.etCalibrationAngle);
        mCalibrateDeviceAngle.setText(String.valueOf(PreferenceUtil.readPrefInt(getActivity(), Common.PREF_INT_CALIBRATE_DEVICE_ANGLE, DEFAULT_CALIBRATION_ANGLE)));
        mMarkDescription = (EditText) mRootView.findViewById(R.id.etDescription);
        mAddress = (EditText) mRootView.findViewById(R.id.etAddress);
        mAddress.setText(PreferenceUtil.readPrefString(getActivity(), PREF_LAST_MAIL_ADDRESS));
        mRootView.findViewById(R.id.btnStartInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStopInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnInsertMark).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSendDataByMail).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSaveData).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnClearData).setOnClickListener(mBtnClickListener);

        for (int i = 0, sizeI = READ_SENSOR.values().length; i < sizeI; i++) {
            READ_SENSOR readSensor = READ_SENSOR.values()[i];

            for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                mTvReadSensorValues[i][j] = (TextView) mRootView.findViewById(readSensor.mDisplayValueResIds[j]);
            }
        }
        mTvRotateAxisAndDegreeValues = new TextView[]{
                (TextView) mRootView.findViewById(R.id.tvAxisX),
                (TextView) mRootView.findViewById(R.id.tvAxisY),
                (TextView) mRootView.findViewById(R.id.tvAxisZ),
                (TextView) mRootView.findViewById(R.id.tvRotateDegree),
        };

        return mRootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults != null && grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveData();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceUtil.writePref(getActivity(), PREF_LAST_MAIL_ADDRESS, mAddress.getText().toString());
        PreferenceUtil.writePref(getActivity(), Common.PREF_INT_CALIBRATE_DEVICE_ANGLE, mCalibrateDeviceAngle.getText().toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mReadInputSensorTask != null) {
            mReadInputSensorTask.cancel(true);
            mReadInputSensorTask = null;
        }
        if (mClearSensorDataTask != null) {
            mClearSensorDataTask.cancel(true);
            mClearSensorDataTask = null;
        }
        if (mSaveSensorDataTask != null) {
            mSaveSensorDataTask.cancel(true);
            mSaveSensorDataTask = null;
        }
    }

    private void startReadInputSensor() {
        if (mReadInputSensorTask == null) {
            mReadInputSensorTask = new ReadInputSensorTask();
            mReadInputSensorTask.execute(DEFAULT_READ_INTERVAL_MS);
            mRootView.findViewById(R.id.llStopInputArea).setVisibility(View.VISIBLE);
            mRootView.findViewById(R.id.btnStartInput).setVisibility(View.GONE);
        }
    }

    private void insertMark() {
        final READ_SENSOR[] readSensors = mReadInputSensorTask.mReadSensors;
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Class> targetClassList = new ArrayList<Class>();
                for (READ_SENSOR readSensor : readSensors) {
                    targetClassList.add(readSensor.mDataClass);
                }
                mDatabase.insertMark(targetClassList.toArray(new Class[0]), mMarkDescription.getText().toString());
            }
        }).start();
    }

    private void sendDataByMail() {
        EditText etAddress = (EditText) mRootView.findViewById(R.id.etAddress);
        final String address = etAddress.getText().toString();

        if (mSaveSensorDataTask == null) {
            if (address != null && address.length() > 0) {
                boolean needRequestPermission = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        needRequestPermission = true;
                    }
                }

                if (needRequestPermission) {
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_STORAGE);
                } else {
                    mSaveSensorDataTask = new SaveSensorDataTask(new OnSaveDataFinishedListener() {
                        @Override
                        public void onSaveDataFinished(File savedDataFile) {
                            startActivity(IntentUtil.getLaunchMailerIntent(address, "sensor data", "", savedDataFile));
                        }
                    });
                    mSaveSensorDataTask.execute();
                }
            } else {
                ToastUtil.showToast(getActivity(), "input \"to\" mail address", Toast.LENGTH_SHORT);
            }
        }
    }

    private void saveData() {
        if (mSaveSensorDataTask == null) {
            boolean needRequestPermission = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    needRequestPermission = true;
                }
            }

            if (needRequestPermission) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_STORAGE);
            } else {
                mSaveSensorDataTask = new SaveSensorDataTask(new OnSaveDataFinishedListener() {
                    @Override
                    public void onSaveDataFinished(File savedDataFile) {
                        ToastUtil.showToast(getActivity(), "saved: \n" + savedDataFile.getAbsolutePath(), Toast.LENGTH_LONG);
                    }
                });
                mSaveSensorDataTask.execute();
            }
        }
    }

    private void clearData() {
        if (mClearSensorDataTask == null) {
            mClearSensorDataTask = new ClearSensorDataTask();
            mClearSensorDataTask.execute();
        }
    }

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

            switch (id) {
                case R.id.btnStartInput:
                    startReadInputSensor();
                    break;
                case R.id.btnStopInput:
                    if (mReadInputSensorTask != null) {
                        mReadInputSensorTask.cancel(false);
                        mReadInputSensorTask = null;

                        mRootView.findViewById(R.id.btnStartInput).setVisibility(View.VISIBLE);
                        mRootView.findViewById(R.id.llStopInputArea).setVisibility(View.GONE);
                    }
                    break;
                case R.id.btnInsertMark:
                    insertMark();
                    break;
                case R.id.btnSendDataByMail:
                    sendDataByMail();
                    break;
                case R.id.btnSaveData:
                    saveData();
                    break;
                case R.id.btnClearData:
                    clearData();
                    break;
            }
        }
    };

    private class ReadInputSensorTask extends AsyncTask<Integer, Void, Void> implements OnCalibrationStatusChangeListener {
        private READ_SENSOR[] mReadSensors;
        private ThreadUtil.OnetimeSemaphore mCalibrationSemaphore = new ThreadUtil.OnetimeSemaphore();
        private int mCalibrateDeviceAngleNumber;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            try {
                mCalibrateDeviceAngleNumber = Integer.valueOf(mCalibrateDeviceAngle.getText().toString());
            } catch (NumberFormatException e) {
                mCalibrateDeviceAngleNumber = DEFAULT_CALIBRATION_ANGLE;
                ToastUtil.showToast(getActivity(), "cannot use " + mCalibrateDeviceAngle.getText().toString() + "\nuse " + DEFAULT_CALIBRATION_ANGLE, Toast.LENGTH_SHORT);
            }
            mUpdateTargetSensorRunnable.run();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mCalibrationSemaphore.stop();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            mCalibrationSemaphore.stop();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            CalibrationCondition condition = new CalibrationCondition(mCalibrateDeviceAngleNumber, CalibrationCondition.HandStatus.HandOpen);
            mGestureDetector.startCalibration(getActivity(), condition, this);
            mCalibrationSemaphore.start();

            Integer intervalMS = integers != null && integers.length > 0 ? integers[0] : DEFAULT_READ_INTERVAL_MS;

            if (intervalMS <= 0) {
                intervalMS = DEFAULT_READ_INTERVAL_MS;
            }

            while (!isCancelled()) {
                if (UhAccessHelper.isEnableDebug()) {
                    LogUtil.d(TAG, "Input Sensor");
                }
                long startTimeMS = System.currentTimeMillis();
                READ_SENSOR[] tempReadSensors = mReadSensors;

                for (int i = 0, size = tempReadSensors.length; i < size; i++) {
                    switch (tempReadSensors[i]) {
                        case PHOTO:
                            if (mUHAccessHelper.readPhotoReflector(mPhotoReflectorData)) {
                                mDatabase.insertData(mPhotoReflectorData);
                            }
                            break;
                        case ANGLE:
                            if (mUHAccessHelper.readAngle(mAngleData)) {
                                mDatabase.insertData(mAngleData);
                            }
                            break;
                        case TEMPERATURE:
                            if (mUHAccessHelper.readTemperature(mTemperatureData)) {
                                mDatabase.insertData(mTemperatureData);
                            }
                            break;
                        case ACCELERATION:
                            if (mUHAccessHelper.readAcceleration(mAccelerationData)) {
                                mDatabase.insertData(mAccelerationData);
                            }
                            break;
                        case GYRO:
                            if (mUHAccessHelper.readGyro(mGyroData)) {
                                mDatabase.insertData(mGyroData);
                            }
                            break;
                        case QUATERNION:
                            if (mUHAccessHelper.readQuaternion(mQuaternionData)) {
                                mDatabase.insertData(mQuaternionData);
                            }
                            break;
                    }
                }
                Activity activity = getActivity();
                if (activity != null) {
                    ThreadUtil.runOnMainThread(activity, mUpdateReadSensorValueRunnable);

                    long endTimeMS = System.currentTimeMillis();
                    long sleepTimeMS = intervalMS - (endTimeMS - startTimeMS);

                    if (sleepTimeMS > 0) {
                        try {
                            Thread.sleep(sleepTimeMS);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public void onCalibrationStatusChange(CalibrationCondition calibrationCondition, CalibrationStatus status) {
            switch (status) {
                case CalibrateSuccess:
                    ToastUtil.showToast(getActivity(), "calibration success", Toast.LENGTH_SHORT);
                    break;
                case CalibrateFail:
                    ToastUtil.showToast(getActivity(), "calibration fail", Toast.LENGTH_SHORT);
                    break;
                case Calibrating:
                default:
                    return;
            }

            mCalibrationSemaphore.stop();
        }
    }

    private interface OnSaveDataFinishedListener {
        void onSaveDataFinished(File savedDataFile);
    }

    private class SaveSensorDataTask extends AsyncTask<Void, Void, File> {
        private ProgressDialog mProgressDialog = null;
        private OnSaveDataFinishedListener mListener;

        public SaveSensorDataTask(OnSaveDataFinishedListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected File doInBackground(Void... aVoid) {
            FileOutputStream outputStream = null;
            GZIPOutputStream gzipOutputStream = null;
            File temporaryFile = null;

            try {
                File temporaryFileDir = new File(getActivity().getExternalCacheDir().getAbsolutePath());
                if (!temporaryFileDir.exists()) {
                    temporaryFileDir.mkdirs();
                }
                Calendar calendar = Calendar.getInstance();
                temporaryFile = new File(
                        temporaryFileDir.getAbsolutePath() + "/" +
                                String.format(TEMPORARY_ZIP_FILE,
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH) + 1,
                                        calendar.get(Calendar.DAY_OF_MONTH),
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        calendar.get(Calendar.SECOND)));
                outputStream = new FileOutputStream(temporaryFile);
                gzipOutputStream = new GZIPOutputStream(outputStream);

                Class[] dataClassArray = {
                        PhotoReflectorData.class,
                        AngleData.class,
                        TemperatureData.class,
                        AccelerationData.class,
                        GyroData.class,
                        QuaternionData.class,
                };
                int[] dataCountArray = {
                        PhotoReflectorData.PHOTO_REFLECTOR_NUM,
                        AngleData.ANGLE_NUM,
                        TemperatureData.TEMPERATURE_NUM,
                        AccelerationData.ACCELERATION_NUM,
                        GyroData.GYRO_NUM,
                        QuaternionData.QUATERNION_NUM,
                };
                Cursor[] dataCursorArray = {
                        mDatabase.getData(dataClassArray[0]),
                        mDatabase.getData(dataClassArray[1]),
                        mDatabase.getData(dataClassArray[2]),
                        mDatabase.getData(dataClassArray[3]),
                        mDatabase.getData(dataClassArray[4]),
                        mDatabase.getData(dataClassArray[5]),
                };
                boolean[] finished = new boolean[dataClassArray.length];
                Integer descriptionIndex = null;

                for (int i = 0, size = dataCursorArray.length; i < size; i++) {
                    dataCursorArray[i].moveToFirst();
                }

                // sort by COLUMN_CDATE
                while (true) {
                    StringBuilder dataLineBuilder = new StringBuilder();
                    Integer oldestDataCursorIndex = null;
                    long tempCData = Long.MAX_VALUE;

                    for (int i = 0, size = dataCursorArray.length; i < size; i++) {
                        finished[i] = dataCursorArray[i].isAfterLast();

                        if (!finished[i]) {
                            long targetCData = dataCursorArray[i].getLong(0);
                            if (tempCData > targetCData) {
                                tempCData = targetCData;
                                oldestDataCursorIndex = i;
                            }
                        }
                    }

                    if (oldestDataCursorIndex == null) {
                        break;
                    } else {
                        Cursor targetCursor = dataCursorArray[oldestDataCursorIndex];
                        Class targetDataClass = dataClassArray[oldestDataCursorIndex];

                        dataLineBuilder.append(targetDataClass.getSimpleName());

                        if (descriptionIndex == null) {
                            descriptionIndex = targetCursor.getColumnIndex(SensorValueDatabase.COLUMN_DESCRIPTION);
                        }
                        String description = targetCursor.getString(descriptionIndex);
                        if (description != null) {
                            // mark row
                            dataLineBuilder.append(",").append(description).append("\n");
                        } else {
                            Constructor constructor = targetDataClass.getConstructor();
                            AbstractSensorData tempInstance = (AbstractSensorData) constructor.newInstance();

                            dataLineBuilder.append(",").append(tempCData);
                            for (int i = 0, size = dataCountArray[oldestDataCursorIndex]; i < size; i++) {
                                String columnName = SensorValueDatabase.getSensorValueColumnName(i);
                                int dataIndex = targetCursor.getColumnIndex(columnName);
                                int dataType = targetCursor.getType(dataIndex);

                                if (dataType == Cursor.FIELD_TYPE_INTEGER) {
                                    dataLineBuilder.append(",").append(targetCursor.getInt(dataIndex));
                                } else if (dataType == Cursor.FIELD_TYPE_FLOAT) {
                                    dataLineBuilder.append(",").append(targetCursor.getFloat(dataIndex));
                                } else if (dataType == Cursor.FIELD_TYPE_STRING) {
                                    dataLineBuilder.append(",").append(targetCursor.getString(dataIndex));
                                }
                            }

//                            if (tempInstance.isSupportCalibration()) {
//                                dataLineBuilder.append(",").append(tempCData);
//                                for (int i = 0, size = dataCountArray[oldestDataCursorIndex]; i < size; i++) {
//                                    String columnName = SensorValueDatabase.getCalibratedSensorValueColumnName(i);
//                                    int dataIndex = targetCursor.getColumnIndex(columnName);
//                                    int dataType = targetCursor.getType(dataIndex);
//
//                                    if (dataType == Cursor.FIELD_TYPE_INTEGER) {
//                                        dataLineBuilder.append(",").append(targetCursor.getInt(dataIndex));
//                                    } else if (dataType == Cursor.FIELD_TYPE_FLOAT) {
//                                        dataLineBuilder.append(",").append(targetCursor.getFloat(dataIndex));
//                                    } else if (dataType == Cursor.FIELD_TYPE_STRING) {
//                                        dataLineBuilder.append(",").append(targetCursor.getString(dataIndex));
//                                    }
//                                }
//                            }
                            dataLineBuilder.append("\n");
                        }

                        byte[] writeData = dataLineBuilder.toString().getBytes();
                        gzipOutputStream.write(writeData, 0, writeData.length);
                        targetCursor.moveToNext();
                    }
                }

                if (gzipOutputStream != null) {
                    gzipOutputStream.close();
                    gzipOutputStream = null;
                }

                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }

                temporaryFile.setReadable(true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } finally {
                if (gzipOutputStream != null) {
                    try {
                        gzipOutputStream.close();
                    } catch (IOException e) {
                        LogUtil.exception(TAG, e);
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        LogUtil.exception(TAG, e);
                    }
                }
            }
            return temporaryFile;
        }

        @Override
        protected void onPostExecute(File aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.dismiss();
            mSaveSensorDataTask = null;

            if (mListener != null) {
                mListener.onSaveDataFinished(aVoid);
            }
        }
    }

    private class ClearSensorDataTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgressDialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... aVoid) {
            READ_SENSOR[] readSensorArray = READ_SENSOR.values();
            ArrayList<Class> readSensorList = new ArrayList<Class>();

            for (READ_SENSOR readSensor : readSensorArray) {
                readSensorList.add(readSensor.mDataClass);
            }

            mDatabase.clearData(readSensorList.toArray(new Class[0]));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.dismiss();
            mClearSensorDataTask = null;
        }
    }

    private Runnable mUpdateReadSensorValueRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0, sizeI = READ_SENSOR.values().length; i < sizeI; i++) {
                READ_SENSOR readSensor = READ_SENSOR.values()[i];

                for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                    LogUtil.d(TAG, mSensorDataArray[i].toString());
                    mTvReadSensorValues[i][j].setText(String.valueOf(mSensorDataArray[i].getRawValue(j)));
                }
            }

            // Quaternionの値から逆算して回転軸と回転角を算出(http://qiita.com/niusounds/items/c2bce33402db22434c82#_reference-1cce530f2aecce08ab5c)
            QuaternionData quaternionData = (QuaternionData) mSensorDataArray[READ_SENSOR.QUATERNION.ordinal()];
            double denominator = Math.sqrt(1 - quaternionData.getRawValue(3));
            if (denominator != 0) {
                double axisX = quaternionData.getRawValue(0) / denominator;
                double axisY = quaternionData.getRawValue(1) / denominator;
                double axisZ = quaternionData.getRawValue(2) / denominator;
                double degree = (Math.acos(quaternionData.getRawValue(3)) * 2) * Math.PI / 180;

                mTvRotateAxisAndDegreeValues[0].setText(String.valueOf(axisX));
                mTvRotateAxisAndDegreeValues[1].setText(String.valueOf(axisY));
                mTvRotateAxisAndDegreeValues[2].setText(String.valueOf(axisZ));
                mTvRotateAxisAndDegreeValues[3].setText(String.valueOf(degree));
            }

            mUpdateTargetSensorRunnable.run();
        }
    };

    private Runnable mUpdateTargetSensorRunnable = new Runnable() {
        @Override
        public void run() {
            if (mReadInputSensorTask != null) {
                ArrayList<READ_SENSOR> readSensorList = new ArrayList<READ_SENSOR>();
                READ_SENSOR[] allReadSensors = READ_SENSOR.values();

                for (READ_SENSOR allReadSensor : allReadSensors) {
                    if (((CheckBox) mRootView.findViewById(allReadSensor.mViewResId)).isChecked()) {
                        readSensorList.add(allReadSensor);
                    }
                }

                mReadInputSensorTask.mReadSensors = readSensorList.toArray(new READ_SENSOR[0]);
            }
        }
    };
}
