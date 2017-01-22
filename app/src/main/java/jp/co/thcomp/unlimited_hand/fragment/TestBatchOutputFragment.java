package jp.co.thcomp.unlimited_hand.fragment;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import jp.co.thcomp.unlimited_hand.R;

public class TestBatchOutputFragment extends AbstractTestFragment {
    private static final int VIEW_TAG_POSITION = "VIEW_TAG_POSITION".hashCode();
    private static final int VIEW_TAG_COMMAND = "VIEW_TAG_COMMAND".hashCode();

    private ListView mLvBatchCommand;
    private BatchCommandTask mBatchCommandTask;

    public TestBatchOutputFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestOutputFragment.
     */
    public static TestBatchOutputFragment newInstance() {
        return new TestBatchOutputFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_batch_output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        ((ListView) mRootView.findViewById(R.id.lvBatchCommandList)).setAdapter(mBatchCommandAdapter);
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
        mRootView.findViewById(R.id.btnStartBatchCommand).setOnClickListener(mBtnClickListener);

        return mRootView;
    }

    private void addCommand(int viewResId) {
        mBatchCommandAdapter.mCommandList.add(viewResId);
        mBatchCommandAdapter.notifyDataSetChanged();
    }

    private void removeCommand(View view) {
        Integer position = (Integer) view.getTag(VIEW_TAG_POSITION);

        if (position != null) {
            mBatchCommandAdapter.mCommandList.remove((int) position);
            mBatchCommandAdapter.notifyDataSetChanged();
        }
    }

    private void startBatchCommand() {
        for (int i = 0, size = mBatchCommandAdapter.getCount(); i < size; i++) {
            int item = (int) mBatchCommandAdapter.getItem(i);
            switch (item) {
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
    }

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

            switch (id) {
                case R.id.btnVibrate:
                case R.id.btnStimulate0:
                case R.id.btnStimulate1:
                case R.id.btnStimulate2:
                case R.id.btnStimulate3:
                case R.id.btnStimulate4:
                case R.id.btnStimulate5:
                case R.id.btnStimulate6:
                case R.id.btnStimulate7:
                case R.id.btnSharpnessUp:
                case R.id.btnSharpnessDown:
                case R.id.btnVoltageUp:
                case R.id.btnVoltageDown:
                    addCommand(id);
                    break;
                case R.id.btnRemoveCommand:
                    removeCommand(view);
                    break;
                case R.id.btnStartBatchCommand:
                    if (mBatchCommandTask == null) {
                        mBatchCommandTask = new BatchCommandTask();
                        mBatchCommandTask.execute();
                    }
                    break;
            }
        }
    };

    private class BatchCommandAdapter extends BaseAdapter {
        private final ArrayList<Integer> mCommandList = new ArrayList<Integer>();

        @Override
        public int getCount() {
            return mCommandList.size();
        }

        @Override
        public Object getItem(int i) {
            return mCommandList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.item_batch_command, viewGroup, false);
            }

            Button btnRemoveCommand = (Button) view.findViewById(R.id.btnRemoveCommand);
            btnRemoveCommand.setTag(VIEW_TAG_POSITION, i);
            btnRemoveCommand.setOnClickListener(mBtnClickListener);
            String title = null;
            int item = (int) getItem(i);
            switch (item) {
                case R.id.btnVibrate:
                    title = "Vibrate";
                    break;
                case R.id.btnStimulate0:
                    title = "Stimulate0";
                    break;
                case R.id.btnStimulate1:
                    title = "Stimulate1";
                    break;
                case R.id.btnStimulate2:
                    title = "Stimulate2";
                    break;
                case R.id.btnStimulate3:
                    title = "Stimulate3";
                    break;
                case R.id.btnStimulate4:
                    title = "Stimulate4";
                    break;
                case R.id.btnStimulate5:
                    title = "Stimulate5";
                    break;
                case R.id.btnStimulate6:
                    title = "Stimulate6";
                    break;
                case R.id.btnStimulate7:
                    title = "Stimulate7";
                    break;
                case R.id.btnSharpnessUp:
                    title = "Sharpness UP";
                    break;
                case R.id.btnSharpnessDown:
                    title = "Sharpness DOWN";
                    break;
                case R.id.btnVoltageUp:
                    title = "Voltage UP";
                    break;
                case R.id.btnVoltageDown:
                    title = "Voltage DOWN";
                    break;
            }
            ((TextView) view.findViewById(R.id.tvCommandName)).setText(title);

            return view;
        }
    }

    private BatchCommandAdapter mBatchCommandAdapter = new BatchCommandAdapter();

    private class BatchCommandTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... avoid) {
            startBatchCommand();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                super.onPostExecute(aVoid);
            } finally {
                mProgressDialog.dismiss();
                mBatchCommandTask = null;
            }
        }
    }
}
