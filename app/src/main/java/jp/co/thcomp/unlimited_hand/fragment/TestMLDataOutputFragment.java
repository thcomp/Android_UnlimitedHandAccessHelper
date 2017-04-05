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
    private static final String TAG = TestMLDataOutputFragment.class.getSimpleName();
    private static final String TEMPORARY_ZIP_FILE = "sensor_data_%1$04d%2$02d%3$02d_%4$02d%5$02d%6$02d.gzip";
    private static final String PREF_LAST_MAIL_ADDRESS = "PREF_LAST_MAIL_ADDRESS";
    private static final int REQUEST_CODE_WRITE_STORAGE = "REQUEST_CODE_WRITE_STORAGE".hashCode() & 0x0000FFFF;
    private static final int DEFAULT_READ_FPS = 30;
    private static final int DEFAULT_READ_INTERVAL_MS = (int) (1000 / DEFAULT_READ_FPS);

    private enum READ_SENSOR {
        PHOTO(R.id.cbPhotoSensor, true, PhotoReflectorData.class, UhAccessHelper.POLLING_PHOTO_REFLECTOR,
                R.id.tvPhotoSensor0, R.id.tvPhotoSensor1, R.id.tvPhotoSensor2, R.id.tvPhotoSensor3,
                R.id.tvPhotoSensor4, R.id.tvPhotoSensor5, R.id.tvPhotoSensor6, R.id.tvPhotoSensor7),
        ANGLE(R.id.cbAngle, false, AngleData.class, UhAccessHelper.POLLING_ANGLE,
                R.id.tvAngle0, R.id.tvAngle1, R.id.tvAngle2),
        TEMPERATURE(R.id.cbTemperature, false, TemperatureData.class, UhAccessHelper.POLLING_TEMPERATURE, R.id.tvTemperature0),
        ACCELERATION(R.id.cbAcceleration, true, AccelerationData.class, UhAccessHelper.POLLING_ACCELERATION,
                R.id.tvAcceleration0, R.id.tvAcceleration1, R.id.tvAcceleration2),
        GYRO(R.id.cbGyro, true, GyroData.class, UhAccessHelper.POLLING_GYRO,
                R.id.tvGyro0, R.id.tvGyro1, R.id.tvGyro2),
        QUATERNION(R.id.cbQuaternion, false, QuaternionData.class, UhAccessHelper.POLLING_QUATERNION,
                R.id.tvQuaternion0, R.id.tvQuaternion1, R.id.tvQuaternion2, R.id.tvQuaternion3),;

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
    private EditText mFingerValue;
    private EditText mAddress;
    private TextView[][] mTvReadSensorValues = {
            new TextView[PhotoReflectorData.PHOTO_REFLECTOR_NUM],
            new TextView[AngleData.ANGLE_NUM],
            new TextView[TemperatureData.TEMPERATURE_NUM],
            new TextView[AccelerationData.ACCELERATION_NUM],
            new TextView[GyroData.GYRO_NUM],
            new TextView[QuaternionData.QUATERNION_NUM],
    };
    private SaveSensorDataTask mSaveSensorDataTask = null;
    private ClearSensorDataTask mClearSensorDataTask = null;
    private ArrayList<AbstractSensorData> mSensorDataList = null;

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

        mFingerValue = (EditText) mRootView.findViewById(R.id.etFingerValue);
        mAddress = (EditText) mRootView.findViewById(R.id.etAddress);
        mAddress.setText(PreferenceUtil.readPrefString(getActivity(), PREF_LAST_MAIL_ADDRESS));
        mRootView.findViewById(R.id.btnStartInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStopInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSendDataByMail).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSaveData).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnClearData).setOnClickListener(mBtnClickListener);

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
                pollingFlag += ((CheckBox) mRootView.findViewById(readSensor.mViewResId)).isChecked() ? readSensor.mPollingFlagValue : 0;
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
                    } else if (sensorDataArray[i] instanceof AngleData) {
                        tempSensorDataList.add(new AngleData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof TemperatureData) {
                        tempSensorDataList.add(new TemperatureData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof AccelerationData) {
                        tempSensorDataList.add(new AccelerationData(sensorDataArray[i]));
                    } else if (sensorDataArray[i] instanceof GyroData) {
                        tempSensorDataList.add(new GyroData(sensorDataArray[i]));
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


                    Constructor constructor = targetDataClass.getConstructor();
                    AbstractMLSensorData tempInstance = (AbstractMLSensorData) constructor.newInstance();

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

    private Runnable mInsertSensorValueRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<AbstractSensorData> tempSensorDataList = mSensorDataList;

            if (tempSensorDataList != null && tempSensorDataList.size() > 0) {
                MLSensorData mlSensorData = new MLSensorData();
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
                    }
                }
                mlSensorData.label = mFingerValue.getText().toString();

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
                } else if (tempSensorData instanceof AccelerationData) {
                    readSensor = READ_SENSOR.ACCELERATION;
                } else if (tempSensorData instanceof GyroData) {
                    readSensor = READ_SENSOR.GYRO;
                }

                if (readSensor != null) {
                    for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                        LogUtil.d(TAG, tempSensorData.toString());
                        mTvReadSensorValues[readSensor.ordinal()][j].setText(String.valueOf(tempSensorData.getRawValue(j)));
                    }
                }
            }
        }
    };
}
