package jp.co.thcomp.unlimited_hand.fragment;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.CalibrationStatus;
import jp.co.thcomp.unlimitedhand.OnCalibrationStatusChangeListener;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
import jp.co.thcomp.util.ToastUtil;

public class TestGestureDetectorFragment extends AbstractTestFragment {
    private static final String TAG = TestGestureDetectorFragment.class.getSimpleName();

    private UhGestureDetector mUhGestureDetector;
    private ProgressDialog mCalibratingProgressDialog;

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

        mCalibratingProgressDialog = new ProgressDialog(getActivity());
        mCalibratingProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mCalibratingProgressDialog.setTitle("calibrating");
        mCalibratingProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (!mUhGestureDetector.isCalibrated()) {
                    mUhGestureDetector.stopCalibration();
                }
            }
        });

        mUhGestureDetector = new UhGestureDetector(mUHAccessHelper, UhGestureDetector.WearDevice.RightArm);
        ((SwitchCompat) mRootView.findViewById(R.id.swtEnableGestureDetector)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    if (!mCalibratingProgressDialog.isShowing()) {
                        mCalibratingProgressDialog.show();

                        mUhGestureDetector.startCalibration(getActivity(), new OnCalibrationStatusChangeListener() {
                            @Override
                            public void onCalibrationStatusChange(CalibrationStatus status) {
                                switch (status) {
                                    case CalibrateSuccess:
                                        mUhGestureDetector.setGestureListener(mGestureListener);
                                        mUhGestureDetector.startGestureListening();
                                        break;
                                    case CalibrateFail:
                                        ToastUtil.showToast(getActivity(), "fail to calibrate Unlimited Hand", Toast.LENGTH_LONG);
                                        compoundButton.setChecked(false);
                                        break;
                                    case Calibrating:
                                    default:
                                        return;
                                }

                                mCalibratingProgressDialog.hide();
                            }
                        });
                    }
                } else {
                    mUhGestureDetector.stopGestureListening();
                }
            }
        });

        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mCalibratingProgressDialog.dismiss();
        mCalibratingProgressDialog = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private UhGestureDetector.OnGestureListener mGestureListener = new UhGestureDetector.OnGestureListener() {

    };
}
