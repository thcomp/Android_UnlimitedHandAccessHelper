package jp.co.thcomp.unlimited_hand.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.UhGestureDetector;
import jp.co.thcomp.unlimitedhand.UhGestureDetector2;
import jp.co.thcomp.unlimitedhand.data.HandData;
import jp.co.thcomp.util.PreferenceUtil;

public class TestGestureDetector2Fragment extends AbstractTestFragment {
    private static final String TAG = TestGestureDetector2Fragment.class.getSimpleName();
    private static final String PREF_INT_COMBINE_SENSOR_DATA_COUNT = "PREF_INT_COMBINE_SENSOR_DATA_COUNT";

    private enum DefaultMlAsset {
        RightArm(UhGestureDetector.WearDevice.RightArm, "defaultPb/right_arm/saved_data.pb"),
        LeftArm(UhGestureDetector.WearDevice.LeftArm, "defaultPb/left_arm/saved_data.pb"),
        RightFoot(UhGestureDetector.WearDevice.RightFoot, "defaultPb/right_foot/saved_data.pb"),
        LeftFoot(UhGestureDetector.WearDevice.LeftFoot, "defaultPb/left_foot/saved_data.pb"),;

        public UhGestureDetector.WearDevice wearDevice;
        public String mlAsset;

        DefaultMlAsset(UhGestureDetector.WearDevice wearDevice, String mlAsset) {
            this.wearDevice = wearDevice;
            this.mlAsset = mlAsset;
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

        int combineSensorDataCount = PreferenceUtil.readPrefInt(getActivity(), PREF_INT_COMBINE_SENSOR_DATA_COUNT, 1);
        mUhGestureDetector = new UhGestureDetector2(getActivity(), mUHAccessHelper, DefaultMlAsset.RightArm.wearDevice, DefaultMlAsset.RightArm.mlAsset, combineSensorDataCount);
        mUhGestureDetector.setFingerStatusListener(mGestureListener);
        mUhGestureDetector.startListening();
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
        mUhGestureDetector.startListening();
    }

    @Override
    public void onPause() {
        super.onPause();

        mUhGestureDetector.stopListening();

        EditText etCombineSensorData = (EditText) mRootView.findViewById(R.id.etCombineSensorDataCount);
        try {
            int combineSensorDataCount = Integer.valueOf(etCombineSensorData.getText().toString());
            PreferenceUtil.writePref(getActivity(), PREF_INT_COMBINE_SENSOR_DATA_COUNT, combineSensorDataCount);
        } catch (NumberFormatException e) {
            // 処理なし
        }
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
