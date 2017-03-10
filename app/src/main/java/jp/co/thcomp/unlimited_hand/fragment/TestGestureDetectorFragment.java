package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.CalibrationCondition;
import jp.co.thcomp.unlimitedhand.CalibrationStatus;
import jp.co.thcomp.unlimitedhand.OnCalibrationStatusChangeListener;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
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

        new CalibrationTask(new CalibrationCondition(0, CalibrationCondition.HandStatus.HandOpen)) {
            @Override
            protected void onPostExecute(Void o) {
                super.onPostExecute(o);

                Activity activity = getActivity();
                if ((activity != null) && !activity.isFinishing()) {
                    if (mResult != CalibrationStatus.CalibrateSuccess) {
                        ToastUtil.showToast(getActivity(), "fail to calibrate: " + CalibrationCondition.HandStatus.HandOpen.name(), Toast.LENGTH_SHORT);
                    }

                    new CalibrationTask(new CalibrationCondition(0, CalibrationCondition.HandStatus.HandClose)) {
                        @Override
                        protected void onPostExecute(Void o) {
                            super.onPostExecute(o);

                            Activity activity = getActivity();
                            if ((activity != null) && !activity.isFinishing()) {
                                if (mResult != CalibrationStatus.CalibrateSuccess) {
                                    ToastUtil.showToast(getActivity(), "fail to calibrate: " + CalibrationCondition.HandStatus.HandClose.name(), Toast.LENGTH_SHORT);
                                }

                                new CalibrationTask(new CalibrationCondition(0, CalibrationCondition.HandStatus.PickObject)) {
                                    @Override
                                    protected void onPostExecute(Void o) {
                                        super.onPostExecute(o);

                                        Activity activity = getActivity();
                                        if ((activity != null) && !activity.isFinishing()) {
                                            if (mResult != CalibrationStatus.CalibrateSuccess) {
                                                ToastUtil.showToast(getActivity(), "fail to calibrate: " + CalibrationCondition.HandStatus.PickObject.name(), Toast.LENGTH_SHORT);
                                            }

                                            mUhGestureDetector = new UhGestureDetector(activity, mUHAccessHelper, UhGestureDetector.WearDevice.RightArm);
                                            mUhGestureDetector.setGestureListener(mGestureListener);
                                            mUhGestureDetector.startGestureListening();
                                        }
                                    }

                                }.execute();
                            }
                        }
                    }.execute();
                }
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
        return mRootView;
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
            int[] allFingerResId = new int[]{
                    R.id.vThumb,
                    R.id.vIndex,
                    R.id.vMiddle,
                    R.id.vRing,
                    R.id.vPinky,
            };

            for(int i=0, size=allFingerCondition.length; i<size; i++){
                int color = Color.WHITE;
                switch (allFingerCondition[i]){
                    case Straight:
                        color = Color.LTGRAY;
                        break;
                    case SoftCurve:
                        color = Color.GREEN;
                        break;
                    case HardCurve:
                        color = Color.BLUE;
                        break;
                }
                mRootView.findViewById(allFingerResId[i]).setBackgroundColor(color);
            }
        }
    };

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
                    builder.setMessage("Please open your hand and touch \"Start\" button");
                    break;
                case HandClose:
                    builder.setMessage("Please close your hand and touch \"Start\" button");
                    break;
                case PickObject:
                    builder.setMessage("Please form your hand to pick and touch \"Start\" button");
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
            if(mCalibratingDialog != null){
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
