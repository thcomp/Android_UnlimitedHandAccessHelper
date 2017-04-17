package jp.co.thcomp.unlimited_hand.fragment;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
import jp.co.thcomp.unlimitedhand.UhGestureDetector2;
import jp.co.thcomp.unlimitedhand.data.HandData;
import jp.co.thcomp.util.PreferenceUtil;
import jp.co.thcomp.util.RuntimePermissionUtil;
import jp.co.thcomp.util.ToastUtil;

public class TestGestureDetector2Fragment extends AbstractTestFragment {
    private static final String TAG = TestGestureDetector2Fragment.class.getSimpleName();
    private static final String PREF_INT_COMBINE_SENSOR_DATA_COUNT = "PREF_INT_COMBINE_SENSOR_DATA_COUNT";

    private enum DefaultMlAsset {
        RightArm(UhGestureDetector.WearDevice.RightArm, "file:///android_asset/DefaultPb/right_arm/saved_data.pb"),
        LeftArm(UhGestureDetector.WearDevice.LeftArm, "file:///android_asset/DefaultPb/left_arm/saved_data.pb"),
        RightFoot(UhGestureDetector.WearDevice.RightFoot, "file:///android_asset/DefaultPb/right_foot/saved_data.pb"),
        LeftFoot(UhGestureDetector.WearDevice.LeftFoot, "file:///android_asset/DefaultPb/left_foot/saved_data.pb"),;

        public UhGestureDetector.WearDevice wearDevice;
        public String mlAsset;

        DefaultMlAsset(UhGestureDetector.WearDevice wearDevice, String mlAsset) {
            this.wearDevice = wearDevice;
            this.mlAsset = mlAsset;
        }
    }

    private enum ExternalMlAsset {
        RightArm(UhGestureDetector.WearDevice.RightArm, "/ExternalPb/right_arm/saved_data.pb"),
        LeftArm(UhGestureDetector.WearDevice.LeftArm, "/ExternalPb/left_arm/saved_data.pb"),
        RightFoot(UhGestureDetector.WearDevice.RightFoot, "/ExternalPb/right_foot/saved_data.pb"),
        LeftFoot(UhGestureDetector.WearDevice.LeftFoot, "/ExternalPb/left_foot/saved_data.pb"),;

        public UhGestureDetector.WearDevice wearDevice;
        //public String mlAsset;
        public String mlAssetInExternalStorage;

        ExternalMlAsset(UhGestureDetector.WearDevice wearDevice, String mlAsset) {
            this.wearDevice = wearDevice;
            this.mlAssetInExternalStorage = Environment.getExternalStorageDirectory() + mlAsset;
        }

        boolean exist() {
            return new File(mlAssetInExternalStorage).exists();
        }
    }

    private UhGestureDetector2 mUhGestureDetector;

    public TestGestureDetector2Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestInputFragment.
     */
    public static TestGestureDetector2Fragment newInstance() {
        return new TestGestureDetector2Fragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        final int combineSensorDataCount = PreferenceUtil.readPrefInt(activity, PREF_INT_COMBINE_SENSOR_DATA_COUNT, 1);

        if (ExternalMlAsset.RightArm.exist()) {
            RuntimePermissionUtil.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    new RuntimePermissionUtil.OnRequestPermissionsResultListener() {
                        @Override
                        public void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                            if (grantResults != null && grantResults.length > 0) {
                                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                    mUhGestureDetector = new UhGestureDetector2(activity, mUHAccessHelper, ExternalMlAsset.RightArm.wearDevice, ExternalMlAsset.RightArm.mlAssetInExternalStorage, combineSensorDataCount);
                                    ToastUtil.showToast(activity, "use external Protocol Buffer file for ML", Toast.LENGTH_SHORT);
                                } else {
                                    mUhGestureDetector = new UhGestureDetector2(activity, mUHAccessHelper, DefaultMlAsset.RightArm.wearDevice, DefaultMlAsset.RightArm.mlAsset, combineSensorDataCount);
                                    ToastUtil.showToast(activity, "use internal Protocol Buffer file for ML", Toast.LENGTH_SHORT);
                                }

                                mUhGestureDetector.setFingerStatusListener(mGestureListener);
                                mUhGestureDetector.startListening();
                            }
                        }
                    }
            );
        } else {
            mUhGestureDetector = new UhGestureDetector2(activity, mUHAccessHelper, DefaultMlAsset.RightArm.wearDevice, DefaultMlAsset.RightArm.mlAsset, combineSensorDataCount);
            mUhGestureDetector.setFingerStatusListener(mGestureListener);
            mUhGestureDetector.startListening();

            ToastUtil.showToast(activity, "use internal Protocol Buffer file for ML", Toast.LENGTH_SHORT);
        }
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_gesture_detector2;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = super.onCreateView(inflater, container, savedInstanceState);

        int combineSensorDataCount = PreferenceUtil.readPrefInt(getActivity(), PREF_INT_COMBINE_SENSOR_DATA_COUNT, 1);
        EditText etCombineSensorData = (EditText) mRootView.findViewById(R.id.etCombineSensorDataCount);
        etCombineSensorData.setText(String.valueOf(combineSensorDataCount));
        mRootView.findViewById(R.id.btnUpdateCombineSensorDataCount).setOnClickListener(mClickListener);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUhGestureDetector != null) {
            mUhGestureDetector.startListening();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUhGestureDetector != null) {
            mUhGestureDetector.stopListening();
        }

        EditText etCombineSensorData = (EditText) mRootView.findViewById(R.id.etCombineSensorDataCount);
        try {
            int combineSensorDataCount = Integer.valueOf(etCombineSensorData.getText().toString());
            PreferenceUtil.writePref(getActivity(), PREF_INT_COMBINE_SENSOR_DATA_COUNT, combineSensorDataCount);
        } catch (NumberFormatException e) {
            // 処理なし
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        RuntimePermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private UhGestureDetector.OnFingerStatusListener mGestureListener = new UhGestureDetector.OnFingerStatusListener() {
        @Override
        public void onFingerStatusChanged(UhGestureDetector.WearDevice wearDevice, long index, HandData data) {
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
                switch (allFingerCondition[i]) {
                    case Straight:
                        mRootView.findViewById(allFingerResId[i][0]).setVisibility(View.VISIBLE);
                        mRootView.findViewById(allFingerResId[i][1]).setVisibility(View.VISIBLE);
                        break;
                    case HardCurve:
                        mRootView.findViewById(allFingerResId[i][0]).setVisibility(View.INVISIBLE);
                        mRootView.findViewById(allFingerResId[i][1]).setVisibility(View.INVISIBLE);
                        break;
                    default:   // SoftCurve
                        mRootView.findViewById(allFingerResId[i][0]).setVisibility(View.VISIBLE);
                        mRootView.findViewById(allFingerResId[i][1]).setVisibility(View.INVISIBLE);
                        break;
                }
            }
        }
    };

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.btnUpdateCombineSensorDataCount) {
                EditText etCombineSensorData = (EditText) mRootView.findViewById(R.id.etCombineSensorDataCount);
                try {
                    int combineSensorDataCount = Integer.valueOf(etCombineSensorData.getText().toString());
                    mUhGestureDetector.stopListening();
                    mUhGestureDetector = new UhGestureDetector2(getActivity(), mUHAccessHelper, DefaultMlAsset.RightArm.wearDevice, DefaultMlAsset.RightArm.mlAsset, combineSensorDataCount);
                    mUhGestureDetector.setFingerStatusListener(mGestureListener);
                    mUhGestureDetector.startListening();
                } catch (NumberFormatException e) {
                    // 処理なし
                }
            }
        }
    };
}
