package jp.co.thcomp.unlimited_hand.fragment;


import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimited_hand.SensorValueDatabase;
import jp.co.thcomp.unlimitedhand.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.AccelerationData;
import jp.co.thcomp.unlimitedhand.AngleData;
import jp.co.thcomp.unlimitedhand.GyroData;
import jp.co.thcomp.unlimitedhand.PhotoSensorData;
import jp.co.thcomp.unlimitedhand.QuaternionData;
import jp.co.thcomp.unlimitedhand.TemperatureData;
import jp.co.thcomp.util.ThreadUtil;

public class TestInputFragment extends AbstractTestFragment {
    private static final int DEFAULT_READ_FPS = 30;
    private static final int DEFAULT_READ_INTERVAL_MS = (int) (1000 / DEFAULT_READ_FPS);

    private enum READ_SENSOR {
        PHOTO(R.id.cbPhotoSensor, PhotoSensorData.class,
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

    private SensorValueDatabase mDatabase;
    private ReadInputSensorTask mReadInputSensorTask;
    private EditText mMarkDescription;
    private PhotoSensorData mPhotoSensorData = new PhotoSensorData();
    private AngleData mAngleData = new AngleData();
    private TemperatureData mTemperatureData = new TemperatureData();
    private AccelerationData mAccelerationData = new AccelerationData();
    private GyroData mGyroData = new GyroData();
    private QuaternionData mQuaternionData = new QuaternionData();
    private TextView[][] mTvReadSensorValues = {
            new TextView[PhotoSensorData.PHOTO_SENSOR_NUM],
            new TextView[AngleData.ANGLE_NUM],
            new TextView[TemperatureData.TEMPERATURE_NUM],
            new TextView[AccelerationData.ACCELERATION_NUM],
            new TextView[GyroData.GYRO_NUM],
            new TextView[QuaternionData.QUATERNION_NUM],
    };
    private AbstractSensorData[] mSensorDataArray = {
            mPhotoSensorData,
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

        mMarkDescription = (EditText) mRootView.findViewById(R.id.etDescription);
        mRootView.findViewById(R.id.btnStartInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStopInput).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnInsertMark).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSendDataByMail).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnClearData).setOnClickListener(mBtnClickListener);

        for (int i = 0, sizeI = READ_SENSOR.values().length; i < sizeI; i++) {
            READ_SENSOR readSensor = READ_SENSOR.values()[i];

            for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                mTvReadSensorValues[i][j] = (TextView) mRootView.findViewById(readSensor.mDisplayValueResIds[j]);
            }
        }

        return mRootView;
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
    }

    private void clearData() {

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
                case R.id.btnClearData:
                    clearData();
                    break;
            }
        }
    };

    private class ReadInputSensorTask extends AsyncTask<Integer, Void, Void> {
        private READ_SENSOR[] mReadSensors;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mUpdateTargetSensorRunnable.run();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            Integer intervalMS = integers != null && integers.length > 0 ? integers[0] : DEFAULT_READ_INTERVAL_MS;

            if (intervalMS <= 0) {
                intervalMS = DEFAULT_READ_INTERVAL_MS;
            }

            while (!isCancelled()) {
                long startTimeMS = System.currentTimeMillis();
                READ_SENSOR[] tempReadSensors = mReadSensors;

                for (int i = 0, size = tempReadSensors.length; i < size; i++) {
                    switch (tempReadSensors[i]) {
                        case PHOTO:
                            if (mUHAccessHelper.readPhotoSensor(mPhotoSensorData)) {
                                mDatabase.insertData(mPhotoSensorData);
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
                ThreadUtil.runOnMainThread(getActivity(), mUpdateReadSensorValueRunnable);

                long endTimeMS = System.currentTimeMillis();
                long sleepTimeMS = intervalMS - (endTimeMS - startTimeMS);

                if (sleepTimeMS > 0) {
                    try {
                        Thread.sleep(sleepTimeMS);
                    } catch (InterruptedException e) {
                    }
                }
            }

            return null;
        }
    }

    private Runnable mUpdateReadSensorValueRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0, sizeI = READ_SENSOR.values().length; i < sizeI; i++) {
                READ_SENSOR readSensor = READ_SENSOR.values()[i];

                for (int j = 0, sizeJ = readSensor.mDisplayValueResIds.length; j < sizeJ; j++) {
                    mTvReadSensorValues[i][j].setText(String.valueOf(mSensorDataArray[i].getValue(j)));
                }
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

                for (int i = 0, size = allReadSensors.length; i < size; i++) {
                    if (((CheckBox) mRootView.findViewById(allReadSensors[i].mViewResId)).isChecked()) {
                        readSensorList.add(allReadSensors[i]);
                    }
                }

                mReadInputSensorTask.mReadSensors = readSensorList.toArray(new READ_SENSOR[0]);
            }
        }
    };
}
