package jp.co.thcomp.unlimited_hand.fragment;


import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import jp.co.thcomp.unlimited_hand.R;
import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.ToastUtil;

public class TestOutputFragment extends AbstractTestFragment {
    public TestOutputFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestOutputFragment.
     */
    public static TestOutputFragment newInstance() {
        return new TestOutputFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        mRootView.findViewById(R.id.btnVibrate).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate0).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate1).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate2).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate3).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate4).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate5).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate6).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnStimulate7).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSharpnessUp).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnSharpnessDown).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnVoltageUp).setOnClickListener(mBtnClickListener);
        mRootView.findViewById(R.id.btnVoltageDown).setOnClickListener(mBtnClickListener);

        return mRootView;
    }

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

            switch (id) {
                case R.id.btnVibrate:
                    mUHAccessHelper.vibrate();
                    break;
                case R.id.btnStimulate0:
                    mUHAccessHelper.electricMuscleStimulation(0);
                    break;
                case R.id.btnStimulate1:
                    mUHAccessHelper.electricMuscleStimulation(1);
                    break;
                case R.id.btnStimulate2:
                    mUHAccessHelper.electricMuscleStimulation(2);
                    break;
                case R.id.btnStimulate3:
                    mUHAccessHelper.electricMuscleStimulation(3);
                    break;
                case R.id.btnStimulate4:
                    mUHAccessHelper.electricMuscleStimulation(4);
                    break;
                case R.id.btnStimulate5:
                    mUHAccessHelper.electricMuscleStimulation(5);
                    break;
                case R.id.btnStimulate6:
                    mUHAccessHelper.electricMuscleStimulation(6);
                    break;
                case R.id.btnStimulate7:
                    mUHAccessHelper.electricMuscleStimulation(7);
                    break;
                case R.id.btnSharpnessUp:
                    mUHAccessHelper.upSharpnessLevel();
                    break;
                case R.id.btnSharpnessDown:
                    mUHAccessHelper.downSharpnessLevel();
                    break;
                case R.id.btnVoltageUp:
                    mUHAccessHelper.upVoltageLevel();
                    break;
                case R.id.btnVoltageDown:
                    mUHAccessHelper.downVoltageLevel();
                    break;
            }
        }
    };
}
