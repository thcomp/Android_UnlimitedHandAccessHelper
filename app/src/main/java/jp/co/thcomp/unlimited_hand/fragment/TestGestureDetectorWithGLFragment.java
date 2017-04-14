package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import jp.co.thcomp.droidsearch3d.Droid3DCG;
import jp.co.thcomp.glsurfaceview.GLDrawView;
import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
import jp.co.thcomp.unlimitedhand.data.AngleData;
import jp.co.thcomp.util.ThreadUtil;

public class TestGestureDetectorWithGLFragment extends AbstractTestFragment {
    private static final String TAG = TestGestureDetectorWithGLFragment.class.getSimpleName();

    private GLDrawView mDrawView;
    private SwitchCompat mSwtListenAngle;
    private Droid3DCG mDroid3D;
    private AngleData mBaseAngle = new AngleData();
    private UhGestureDetector mUhGestureDetector;

    public TestGestureDetectorWithGLFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestInputFragment.
     */
    public static TestGestureDetectorWithGLFragment newInstance() {
        return new TestGestureDetectorWithGLFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUhGestureDetector = new UhGestureDetector(getActivity(), mUHAccessHelper, UhGestureDetector.WearDevice.RightArm);
        mUhGestureDetector.setDeviceAngleListener(mDeviceAngleListener);
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_gesture_detector_with_gl;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Activity activity = getActivity();

        // Inflate the layout for this fragment
        mRootView = super.onCreateView(inflater, container, savedInstanceState);
        (mSwtListenAngle = (SwitchCompat) mRootView.findViewById(R.id.swtListenAngle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ThreadUtil.runOnWorkThread(activity, new Runnable() {
                        @Override
                        public void run() {
                            mUHAccessHelper.readAngle(mBaseAngle);
                            if (!activity.isFinishing()) {
                                ThreadUtil.runOnMainThread(getActivity(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mSwtListenAngle.isChecked()) {
                                            mUhGestureDetector.startListening();
                                        }
                                    }
                                });
                            }
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    }).start();
                } else {
                    mUhGestureDetector.stopListening();
                }
            }
        });
        mDrawView = (GLDrawView) mRootView.findViewById(R.id.dvRobot);
        mDrawView.startRenderer(null, activity.getApplicationContext());

        mDroid3D = new Droid3DCG(mDrawView);
        mDrawView.addDrawParts(mDroid3D);
        //mDroid3D.moveTo((int)(mDroid3D.getDroidWidth(activity) / 2), (int)(mDroid3D.getDroidHeight(activity) / 2));
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDrawView.onResume();
        if (mSwtListenAngle.isChecked()) {
            mUhGestureDetector.startListening();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mDrawView.onPause();
        mUhGestureDetector.stopListening();
    }

    private UhGestureDetector.OnDeviceAngleListener mDeviceAngleListener = new UhGestureDetector.OnDeviceAngleListener() {
        @Override
        public void onDeviceAngleChanged(UhGestureDetector.WearDevice wearDevice, long index, AngleData angleData) {
            int axisX = angleData.getRawValue(0) - mBaseAngle.getRawValue(0);
            int axisY = angleData.getRawValue(1) - mBaseAngle.getRawValue(1);
            int axisZ = angleData.getRawValue(2) - mBaseAngle.getRawValue(2);

            mDroid3D.rotateDroid(axisX, Droid3DCG.AxisX);
            mDroid3D.rotateDroid(axisY, Droid3DCG.AxisY);
            mDroid3D.rotateDroid(axisZ, Droid3DCG.AxisZ);
        }
    };
}
