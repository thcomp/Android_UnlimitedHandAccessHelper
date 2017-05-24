package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
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
import java.util.zip.GZIPOutputStream;

import jp.co.thcomp.unlimited_hand.MLSensorValueDatabase;
import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimited_hand.data.AbstractMLSensorData;
import jp.co.thcomp.unlimited_hand.data.MLSensorData;
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

public class TestMLDataOutputFragment extends AbstractTestFragment {
    private static final boolean ALWAYS_ENABLE_ALL_SENSOR = true;
    private static final int MAX_SUPPORT_FINGER_CONDITION = UhGestureDetector.FingerCondition.HardCurve.ordinal();
    private static final String TAG = TestMLDataOutputFragment.class.getSimpleName();
    private static final String TEMPORARY_ZIP_FILE = "sensor_data_%1$s%2$s%3$s%4$s%5$s.gzip";
    private static final String PREF_LAST_MAIL_ADDRESS = "PREF_LAST_MAIL_ADDRESS";
    private static final int REQUEST_CODE_WRITE_STORAGE = "REQUEST_CODE_WRITE_STORAGE".hashCode() & 0x0000FFFF;
    private static final int DEFAULT_READ_FPS = 30;
    private static final int DEFAULT_READ_INTERVAL_MS = (int) (1000 / DEFAULT_READ_FPS);

    private enum READ_SENSOR {
        ACCELERATION(R.id.cbAcceleration, true, AccelerationData.class, UhAccessHelper.POLLING_ACCELERATION,
                R.id.tvAcceleration0, R.id.tvAcceleration1, R.id.tvAcceleration2),
        GYRO(R.id.cbGyro, true, GyroData.class, UhAccessHelper.POLLING_GYRO,
                R.id.tvGyro0, R.id.tvGyro1, R.id.tvGyro2),
        PHOTO(R.id.cbPhotoSensor, true, PhotoReflectorData.class, UhAccessHelper.POLLING_PHOTO_REFLECTOR,
                R.id.tvPhotoSensor0, R.id.tvPhotoSensor1, R.id.tvPhotoSensor2, R.id.tvPhotoSensor3,
                R.id.tvPhotoSensor4, R.id.tvPhotoSensor5, R.id.tvPhotoSensor6, R.id.tvPhotoSensor7),
        ANGLE(R.id.cbAngle, true, AngleData.class, UhAccessHelper.POLLING_ANGLE,
                R.id.tvAngle0, R.id.tvAngle1, R.id.tvAngle2),
        TEMPERATURE(R.id.cbTemperature, true, TemperatureData.class, UhAccessHelper.POLLING_TEMPERATURE, R.id.tvTemperature0),
        QUATERNION(R.id.cbQuaternion, true, QuaternionData.class, UhAccessHelper.POLLING_QUATERNION,
                R.id.tvQuaternion0, R.id.tvQuaternion1, R.id.tvQuaternion2, R.id.tvQuaternion3),
        AMBIENT_LIGHT(R.id.cbAmbientLight, true, null, 0, R.id.tvAmbientLight0),;

        int mViewResId;
        boolean mDefaultChecked;
        Class mDataClass;
        int mPollingFlagValue;
        int[] mDisplayValueResIds;

        READ_SENSOR(int viewResId, boolean defaultChecked, Class dataClass, int pollingFlagValue, int... displayValueResIds) {
            mViewResId = viewResId;
            mDefaultChecked = defaultChecked;
            mDataClass = dataClass;
            mPollingFlagValue = pollingFlagValue;
            mDisplayValueResIds = displayValueResIds;
        }
    }

    private MLSensorValueDatabase mDatabase;
    private ReadInputSensorTask mReadInputSensorTask;
    private EditText mThumbValue;
    private EditText mIndexValue;
    private EditText mMiddleValue;
    private EditText mRingValue;
    private EditText mPinkeyValue;
    private EditText mAddress;
    private TextView[][] mTvReadSensorValues = {
            new TextView[AccelerationData.ACCELERATION_NUM],
            new TextView[GyroData.GYRO_NUM],
            new TextView[PhotoReflectorData.PHOTO_REFLECTOR_NUM],
            new TextView[AngleData.ANGLE_NUM],
            new TextView[TemperatureData.TEMPERATURE_NUM],
            new TextView[QuaternionData.QUATERNION_NUM],
            new TextView[1],    // for ambient light
    };
    private SaveSensorDataTask mSaveSensorDataTask = null;
    private ClearSensorDataTask mClearSensorDataTask = null;
    private ArrayList<AbstractSensorData> mSensorDataList = null;
    private Sensor mAmbientLightSensor = null;
    private float mLastAmbientLight = 0f;

    public TestMLDataOutputFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestInputFragment.
     */
    public static TestMLDataOutputFragment newInstance() {
        TestMLDataOutputFragment fragment = new TestMLDataOutputFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = new MLSensorValueDatabase(getContext(), 1);
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAmbientLightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(mLightSensorEventListener, mAmbientLightSensor, UhAccessHelper.DEFAULT_AMBIENT_LIGHT_POLLING);
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_ml_data_output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = super.onCreateView(inflater, container, savedInstanceState);

        if (ALWAYS_ENABLE_ALL_SENSOR) {
            mRootView.findViewById(R.id.llSensorArea).setVisibility(View.GONE);
        } else {
            mRootView.findViewById(R.id.llSensorArea).setVisibility(View.VISIBLE);
        }

        mThumbValue = (EditText) mRootView.findViewById(R.id.etThumbValue);
        mIndexValue = (EditText) mRootView.findViewById(R.id.etIndexValue);
        mMiddleValue = (EditText) mRootView.findViewById(R.id.etMiddleValue);
        mRingValue = (EditText) mRootView.findViewById(R.id.etRingValue);
        mPinkeyValue = (EditText) mRootView.findViewById(R.id.etPinkeyValue);
        mAddress = (EditText) mRootView.findViewById(R.id.etAddress);
        mAddress.setText(PreferenceUtil.readPrefString(getActivity(), PREF_LAST_MAIL_ADDRESS));
        mRootView.findViewById(R.id.btnStartInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStopInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSendDataByMail).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSaveData).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnClearData).setOnClickListener(mBtnClickListener);

        TextView tvFingerConditionDescription = (TextView) mRootView.findViewById(R.id.tvFinderConditionDescription);
        StringBuilder fingerConditionDescription = new StringBuilder();
        for (int i = 0, size = UhGestureDetector.FingerCondition.values().length; i < size; i++) {
            if (i != 0) {
                fingerConditionDescription.append(", ");
            }
            fingerConditionDescription.append(i).append(":").append(UhGestureDetector.FingerCondition.values()[i].name());
        }
        tvFingerConditionDescription.setText(fingerConditionDescription.toString());

        for (int i = 0, sizeI = READ_SENSOR.values().length; i < sizeI; i++) {
            READ_SENSOR readSensor = READ_SENSOR.values()[i];

            ((CheckBox) mRootView.findViewById(readSensor.mViewResId)).setChecked(readSensor.mDefaultChecked);
            for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                mTvReadSensorValues[i][j] = (TextView) mRootView.findViewById(readSensor.mDisplayValueResIds[j]);
            }
        }

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mReadInputSensorTask != null) {
            mReadInputSensorTask.cancel();
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

    private SensorEventListener mLightSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastAmbientLight = event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // 処理なし
        }
    };

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
                        mReadInputSensorTask.cancel();
                        mReadInputSensorTask = null;

                        mRootView.findViewById(R.id.btnStartInput).setVisibility(View.VISIBLE);
                        mRootView.findViewById(R.id.llStopInputArea).setVisibility(View.GONE);
                    }
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

    private class ReadInputSensorTask implements UhAccessHelper.OnSensorPollingListener {
        private boolean mCanceled = false;

        public void execute(Integer... pollingRatePerSecond) {
            if ((pollingRatePerSecond != null) && (pollingRatePerSecond.length > 0) && (pollingRatePerSecond[0] > 0)) {
                mUHAccessHelper.setPollingRatePerSecond(pollingRatePerSecond[0]);
            }

            int pollingFlag = 0;
            for (READ_SENSOR readSensor : READ_SENSOR.values()) {
                if (ALWAYS_ENABLE_ALL_SENSOR) {
                    pollingFlag += readSensor.mPollingFlagValue;
                } else {
                    pollingFlag += ((CheckBox) mRootView.findViewById(readSensor.mViewResId)).isChecked() ? readSensor.mPollingFlagValue : 0;
                }
            }

            mUHAccessHelper.startPollingSensor(this, pollingFlag);
        }

        public void cancel() {
            mCanceled = true;
            mUHAccessHelper.stopPollingSensor(this);
        }

        @Override
        public void onPollSensor(AbstractSensorData[] sensorDataArray) {
            if (!mCanceled) {
                ArrayList<AbstractSensorData> tempSensorDataList = new ArrayList<AbstractSensorData>();

                for (int i = 0, size = sensorDataArray.length; i < size; i++) {
                    if (sensorDataArray[i] instanceof PhotoReflectorData) {
                        tempSensorDataList.add(new PhotoReflectorData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof AccelerationData) {
                        tempSensorDataList.add(new AccelerationData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof GyroData) {
                        tempSensorDataList.add(new GyroData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof AngleData) {
                        tempSensorDataList.add(new AngleData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof TemperatureData) {
                        tempSensorDataList.add(new TemperatureData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof QuaternionData) {
                        tempSensorDataList.add(new QuaternionData(sensorDataArray[i]));
                    }
                }

                mSensorDataList = tempSensorDataList;
                Activity activity = getActivity();
                if (activity != null) {
                    ThreadUtil.runOnMainThread(activity, mUpdateReadSensorValueRunnable);
                }

                new Thread(mInsertSensorValueRunnable).start();
            } else {
                mUHAccessHelper.stopPollingSensor(this);
            }
        }
    }

    private interface OnSaveDataFinishedListener {
        void onSaveDataFinished(File savedDataFile);
    }

    private class SaveSensorDataTask extends AsyncTask<Void, Void, File> {
        private ProgressDialog mProgressDialog = null;
        private OnSaveDataFinishedListener mListener;
        private Editable mThumbEditable;
        private Editable mIndexEditable;
        private Editable mMiddleEditable;
        private Editable mRingEditable;
        private Editable mPinkeyEditable;

        public SaveSensorDataTask(OnSaveDataFinishedListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mThumbEditable = mThumbValue.getText();
            mIndexEditable = mIndexValue.getText();
            mMiddleEditable = mMiddleValue.getText();
            mRingEditable = mRingValue.getText();
            mPinkeyEditable = mPinkeyValue.getText();

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
                temporaryFile = new File(
                        temporaryFileDir.getAbsolutePath() + "/" +
                                String.format(TEMPORARY_ZIP_FILE,
                                        mPinkeyEditable.toString(),
                                        mRingEditable.toString(),
                                        mMiddleEditable.toString(),
                                        mIndexEditable.toString(),
                                        mThumbEditable.toString()));

                outputStream = new FileOutputStream(temporaryFile);
                gzipOutputStream = new GZIPOutputStream(outputStream);

                Class[] dataClassArray = {
                        MLSensorData.class,
                };
                Cursor[] dataCursorArray = {
                        mDatabase.getData(dataClassArray[0]),
                };

                for (int i = 0, size = dataCursorArray.length; i < size; i++) {
                    dataCursorArray[i].moveToFirst();
                }

                // sort by COLUMN_CDATE
                for (int i = 0, sizeI = dataClassArray.length; i < sizeI; i++) {
                    StringBuilder dataLineBuilder = new StringBuilder();

                    Cursor targetCursor = dataCursorArray[i];
                    Class targetDataClass = dataClassArray[i];

                    boolean[] useSensorArray = new boolean[READ_SENSOR.values().length];
                    for (READ_SENSOR readSensor : READ_SENSOR.values()) {
                        if (ALWAYS_ENABLE_ALL_SENSOR) {
                            useSensorArray[readSensor.ordinal()] = true;
                        } else {
                            CheckBox checkBox = (CheckBox) mRootView.findViewById(readSensor.mViewResId);
                            useSensorArray[readSensor.ordinal()] = checkBox.isChecked();
                        }
                    }
                    Constructor constructor = targetDataClass.getConstructor(boolean[].class);
                    AbstractMLSensorData tempInstance = (AbstractMLSensorData) constructor.newInstance(useSensorArray);

                    for (; !targetCursor.isAfterLast(); targetCursor.moveToNext()) {
                        dataLineBuilder.append(tempInstance.getMLSensorData(targetCursor)).append("\n");

                        byte[] writeData = dataLineBuilder.toString().getBytes();
                        gzipOutputStream.write(writeData, 0, writeData.length);
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
                LogUtil.exception(TAG, e);
            } catch (IOException e) {
                LogUtil.exception(TAG, e);
            } catch (NoSuchMethodException e) {
                LogUtil.exception(TAG, e);
            } catch (java.lang.InstantiationException e) {
                LogUtil.exception(TAG, e);
            } catch (IllegalAccessException e) {
                LogUtil.exception(TAG, e);
            } catch (InvocationTargetException e) {
                LogUtil.exception(TAG, e);
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

            mDatabase.clearData();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.dismiss();
            mClearSensorDataTask = null;
        }
    }

    private Runnable mInsertSensorValueRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<AbstractSensorData> tempSensorDataList = mSensorDataList;

            if (tempSensorDataList != null && tempSensorDataList.size() > 0) {
                MLSensorData mlSensorData = new MLSensorData();
                mlSensorData.ambientLightDataArray[0] = String.valueOf(mLastAmbientLight);
                for (AbstractSensorData sensorData : tempSensorDataList) {
                    if (sensorData instanceof PhotoReflectorData) {
                        for (int i = 0, size = PhotoReflectorData.PHOTO_REFLECTOR_NUM; i < size; i++) {
                            mlSensorData.prDataArray[i] = String.valueOf(sensorData.getRawValue(i));
                        }
                    } else if (sensorData instanceof AccelerationData) {
                        for (int i = 0, size = AccelerationData.ACCELERATION_NUM; i < size; i++) {
                            mlSensorData.accelDataArray[i] = String.valueOf(sensorData.getRawValue(i));
                        }
                    } else if (sensorData instanceof GyroData) {
                        for (int i = 0, size = GyroData.GYRO_NUM; i < size; i++) {
                            mlSensorData.gyroDataArray[i] = String.valueOf(sensorData.getRawValue(i));
                        }
                    } else if (sensorData instanceof AngleData) {
                        for (int i = 0, size = AngleData.ANGLE_NUM; i < size; i++) {
                            mlSensorData.angleDataArray[i] = String.valueOf(sensorData.getRawValue(i));
                        }
                    } else if (sensorData instanceof TemperatureData) {
                        for (int i = 0, size = TemperatureData.TEMPERATURE_NUM; i < size; i++) {
                            mlSensorData.temperatureDataArray[i] = String.valueOf(sensorData.getRawValue(i));
                        }
                    } else if (sensorData instanceof QuaternionData) {
                        for (int i = 0, size = QuaternionData.QUATERNION_NUM; i < size; i++) {
                            mlSensorData.quaternionDataArray[i] = String.valueOf(sensorData.getRawValue(i));
                        }
                    }
                }
                String[] fingerValues = {
                        mThumbValue.getText().toString(),
                        mIndexValue.getText().toString(),
                        mMiddleValue.getText().toString(),
                        mRingValue.getText().toString(),
                        mPinkeyValue.getText().toString(),
                };
                int fingerValueInt = 0;

                // 10進数に変換
                int fingerConditionCount = MAX_SUPPORT_FINGER_CONDITION + 1;
                for (int i = 0, size = fingerValues.length; i < size; i++) {
                    int tempFingerValue = 0;
                    try {
                        tempFingerValue = Integer.valueOf(fingerValues[i]);
                        if (UhGestureDetector.FingerCondition.Straight.ordinal() <= tempFingerValue && tempFingerValue <= MAX_SUPPORT_FINGER_CONDITION) {
                            fingerValueInt += (Math.pow(fingerConditionCount, i) * tempFingerValue);
                        }
                    } catch (NumberFormatException e) {
                        // 処理なし
                    }
                }
                mlSensorData.label = String.valueOf(fingerValueInt);

                mDatabase.insertData(mlSensorData);
            }
        }
    };

    private Runnable mUpdateReadSensorValueRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<AbstractSensorData> tempSensorDataList = mSensorDataList;
            for (int i = 0, sizeI = tempSensorDataList.size(); i < sizeI; i++) {
                AbstractSensorData tempSensorData = tempSensorDataList.get(i);
                READ_SENSOR readSensor = null;

                if (tempSensorData instanceof PhotoReflectorData) {
                    readSensor = READ_SENSOR.PHOTO;
                } else if (tempSensorData instanceof AngleData) {
                    readSensor = READ_SENSOR.ANGLE;
                } else if (tempSensorData instanceof TemperatureData) {
                    readSensor = READ_SENSOR.TEMPERATURE;
                } else if (tempSensorData instanceof AccelerationData) {
                    readSensor = READ_SENSOR.ACCELERATION;
                } else if (tempSensorData instanceof GyroData) {
                    readSensor = READ_SENSOR.GYRO;
                } else if (tempSensorData instanceof QuaternionData) {
                    readSensor = READ_SENSOR.QUATERNION;
                }

                if (readSensor != null) {
                    for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                        LogUtil.d(TAG, tempSensorData.toString());
                        mTvReadSensorValues[readSensor.ordinal()][j].setText(String.valueOf(tempSensorData.getRawValue(j)));
                    }
                }
            }

            mTvReadSensorValues[READ_SENSOR.AMBIENT_LIGHT.ordinal()][0].setText(String.valueOf(mLastAmbientLight));
        }
    };
}
