package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import jp.co.thcomp.unlimited_hand.Common;
import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.CalibrationCondition;
import jp.co.thcomp.unlimitedhand.CalibrationStatus;
import jp.co.thcomp.unlimitedhand.OnCalibrationStatusChangeListener;
import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
import jp.co.thcomp.unlimitedhand.data.AbstractSensorData;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.util.PreferenceUtil;
import jp.co.thcomp.util.ThreadUtil;
import jp.co.thcomp.util.ToastUtil;

public class TestGestureDetectorFragment extends AbstractTestFragment {
    private static final String TAG = TestGestureDetectorFragment.class.getSimpleName();

    private UhGestureDetector mUhGestureDetector;

    public TestGestureDetectorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestInputFragment.
     */
    public static TestGestureDetectorFragment newInstance() {
        return new TestGestureDetectorFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUhGestureDetector = new UhGestureDetector(getActivity(), mUHAccessHelper, UhGestureDetector.WearDevice.RightArm);
        mUhGestureDetector.setGestureListener(mGestureListener);
        new DetectCalibrationAngleTask(){
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                final int calibrationAngle = this.mCount > 0 ? this.mCalibrationAngleAve / this.mCount : 0;

                new CalibrationTask(new CalibrationCondition(calibrationAngle, CalibrationCondition.HandStatus.HandOpen)) {
                    @Override
                    protected void onPostExecute(Void o) {
                        super.onPostExecute(o);

                        Activity activity = getActivity();
                        if ((activity != null) && !activity.isFinishing()) {
                            if (mResult != CalibrationStatus.CalibrateSuccess) {
                                ToastUtil.showToast(getActivity(), "fail to calibrate: " + CalibrationCondition.HandStatus.HandOpen.name(), Toast.LENGTH_SHORT);
                            }

                            new CalibrationTask(new CalibrationCondition(calibrationAngle, CalibrationCondition.HandStatus.HandClose)) {
                                @Override
                                protected void onPostExecute(Void o) {
                                    super.onPostExecute(o);

                                    Activity activity = getActivity();
                                    if ((activity != null) && !activity.isFinishing()) {
                                        if (mResult != CalibrationStatus.CalibrateSuccess) {
                                            ToastUtil.showToast(getActivity(), "fail to calibrate: " + CalibrationCondition.HandStatus.HandClose.name(), Toast.LENGTH_SHORT);
                                        }

//                                        new CalibrationTask(new CalibrationCondition(calibrationAngle, CalibrationCondition.HandStatus.PickObject)) {
//                                            @Override
//                                            protected void onPostExecute(Void o) {
//                                                super.onPostExecute(o);
//
//                                                Activity activity = getActivity();
//                                                if ((activity != null) && !activity.isFinishing()) {
//                                                    if (mResult != CalibrationStatus.CalibrateSuccess) {
//                                                        ToastUtil.showToast(getActivity(), "fail to calibrate: " + CalibrationCondition.HandStatus.PickObject.name(), Toast.LENGTH_SHORT);
//                                                    }
//
//                                                    mUhGestureDetector.startGestureListening();
//                                                }
//                                            }
//
//                                        }.execute();
                                        mUhGestureDetector.startGestureListening();
                                    }
                                }
                            }.execute();
                        }
                    }
                }.execute();
            }
        }.execute();
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_gesture_detector;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = super.onCreateView(inflater, container, savedInstanceState);
        mRootView.findViewById(R.id.btnUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etGestureDetectThreshold = (EditText) mRootView.findViewById(R.id.etGestureDetectThreshold);
                String gestureDetectThreshold = etGestureDetectThreshold.getText().toString();
                mUhGestureDetector.setDetectThreshold(Integer.parseInt(gestureDetectThreshold));
                ToastUtil.showToast(getActivity(), "Update gesture detect: " + gestureDetectThreshold, Toast.LENGTH_SHORT);
            }
        });
        ((EditText) mRootView.findViewById(R.id.etGestureDetectThreshold)).setText(String.valueOf(PreferenceUtil.readPrefInt(getActivity(), Common.PREF_INT_GESTURE_DETECT_THRESHOLD, mUhGestureDetector.getDetectThreshold())));
        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        EditText etGestureDetectThreshold = (EditText) mRootView.findViewById(R.id.etGestureDetectThreshold);
        try {
            PreferenceUtil.writePref(getActivity(), Common.PREF_INT_GESTURE_DETECT_THRESHOLD, Integer.parseInt(etGestureDetectThreshold.getText().toString()));
        } catch (NumberFormatException e) {
            // 処理なし
        }
    }

    private UhGestureDetector.OnGestureListener mGestureListener = new UhGestureDetector.OnGestureListener() {
        @Override
        public void onHandStatusChanged(UhGestureDetector.HandData data) {
            UhGestureDetector.FingerCondition[] allFingerCondition = new UhGestureDetector.FingerCondition[]{
                    data.thumb,
                    data.index,
                    data.middle,
                    data.ring,
                    data.pinky,
            };
            int[][] allFingerResId = new int[][]{
                    {R.id.vThumb1, R.id.vThumb2},
                    {R.id.vIndex1, R.id.vIndex2},
                    {R.id.vMiddle1, R.id.vMiddle2},
                    {R.id.vRing1, R.id.vRing2},
                    {R.id.vPinky1, R.id.vPinky2},
            };

            for (int i = 0, size = allFingerCondition.length; i < size; i++) {
                int color = Color.WHITE;
                switch (allFingerCondition[i]) {
                    case Straight:
                        mRootView.findViewById(allFingerResId[i][0]).setVisibility(View.VISIBLE);
                        mRootView.findViewById(allFingerResId[i][1]).setVisibility(View.VISIBLE);
                        break;
                    case SoftCurve:
                        mRootView.findViewById(allFingerResId[i][0]).setVisibility(View.VISIBLE);
                        mRootView.findViewById(allFingerResId[i][1]).setVisibility(View.INVISIBLE);
                        break;
                    case HardCurve:
                        mRootView.findViewById(allFingerResId[i][0]).setVisibility(View.INVISIBLE);
                        mRootView.findViewById(allFingerResId[i][1]).setVisibility(View.INVISIBLE);
                        break;
                }
            }
        }
    };

    private class DetectCalibrationAngleTask extends AsyncTask<Void, Void, Void> implements UhAccessHelper.OnSensorPollingListener {
        protected ProgressDialog mDetectingAngleDialog = null;
        protected ThreadUtil.OnetimeSemaphore mSemaphore = new ThreadUtil.OnetimeSemaphore();
        protected int mCalibrationAngleAve = 0;
        protected int mCount = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Please fix your arm with Unlimited Hand, you want to correct and touch \"START\" button");
            builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDetectingAngleDialog = new ProgressDialog(getActivity());
                    mDetectingAngleDialog.setMessage("Detecting calibration device angle");
                    mDetectingAngleDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mDetectingAngleDialog.setCanceledOnTouchOutside(false);
                    mDetectingAngleDialog.setCancelable(true);
                    mDetectingAngleDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mUHAccessHelper.stopPollingSensor(DetectCalibrationAngleTask.this);
                            mSemaphore.stop();
                        }
                    });
                    mDetectingAngleDialog.show();

                    mUHAccessHelper.startPollingSensor(DetectCalibrationAngleTask.this, UhAccessHelper.POLLING_ANGLE);
                }
            });
            builder.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mSemaphore.start();
            mUHAccessHelper.stopPollingSensor(this);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        public void onPollSensor(AbstractSensorData[] sensorDataArray) {
            if (sensorDataArray != null && sensorDataArray.length > 0) {
                if (sensorDataArray[0] instanceof AngleData) {
                    AngleData angleData = (AngleData) sensorDataArray[0];
                    mCalibrationAngleAve += angleData.getRawValue(0);
                    mCount++;

                    if (mCount >= 10) {
                        mDetectingAngleDialog.dismiss();
                        mSemaphore.stop();
                    }
                }
            }
        }
    }

    private class CalibrationTask extends AsyncTask<Void, Void, Void> implements OnCalibrationStatusChangeListener {
        protected CalibrationCondition mCondition;
        protected CalibrationStatus mResult = CalibrationStatus.Init;
        protected ThreadUtil.OnetimeSemaphore mSemaphore = new ThreadUtil.OnetimeSemaphore();
        protected ProgressDialog mCalibratingDialog = null;

        public CalibrationTask(CalibrationCondition condition) {
            if (condition == null || condition.handStatus == null) {
                throw new NullPointerException("condition == null || condition.handStatus == null");
            }

            mCondition = condition;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            switch (mCondition.handStatus) {
                case HandOpen:
                    builder.setMessage("Please open your hand and touch \"START\" button");
                    break;
                case HandClose:
                    builder.setMessage("Please close your hand and touch \"START\" button");
                    break;
                case PickObject:
                    builder.setMessage("Please form your hand to pick and touch \"START\" button");
                    break;
            }
            builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCalibratingDialog = new ProgressDialog(getActivity());
                    mCalibratingDialog.setMessage(mCondition.handStatus.name() + ": Calibrating");
                    mCalibratingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mCalibratingDialog.setCanceledOnTouchOutside(false);
                    mCalibratingDialog.setCancelable(true);
                    mCalibratingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mSemaphore.stop();
                        }
                    });
                    mCalibratingDialog.show();

                    mUHAccessHelper.startCalibration(getActivity(), mCondition, CalibrationTask.this);
                }
            });
            builder.show();
        }

        @Override
        protected Void doInBackground(Void[] params) {
            mSemaphore.start();
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            super.onPostExecute(o);
            if (mCalibratingDialog != null) {
                mCalibratingDialog.dismiss();
                mCalibratingDialog = null;
            }
        }

        @Override
        public void onCalibrationStatusChange(CalibrationCondition calibrationCondition, CalibrationStatus status) {
            mResult = status;
            mSemaphore.stop();
        }
    }
}
