package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

import jp.co.thcomp.unlimited_hand.AlarmSettingDatabase;
import jp.co.thcomp.unlimited_hand.R;

public class TestAlarmSettingFragment extends AbstractTestFragment {
    private AlarmSettingDatabase mAlarmSettingDatabase;
    private TimeAdapter mHourTimeAdapter;
    private TimeAdapter mMinuteTimeAdapter;

    public TestAlarmSettingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TestAlarmSettingFragment.
     */
    public static TestAlarmSettingFragment newInstance() {
        return new TestAlarmSettingFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlarmSettingDatabase = new AlarmSettingDatabase(getActivity(), 1);
    }

    @Override
    int getLayoutResId() {
        return R.layout.fragment_test_alarm_setting;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Activity activity = getActivity();
        mRootView = super.onCreateView(inflater, container, savedInstanceState);
        mHourTimeAdapter = new TimeAdapter(activity, TimeAdapter.TimeElementType.Hour);
        mMinuteTimeAdapter = new TimeAdapter(activity, TimeAdapter.TimeElementType.Minute);
        ((Spinner)mRootView.findViewById(R.id.spnrHour)).setAdapter(mHourTimeAdapter);
        ((Spinner)mRootView.findViewById(R.id.spnrMinute)).setAdapter(mMinuteTimeAdapter);

        return mRootView;
    }

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

        }
    };

    private class AlarmSettingAdapter extends BaseAdapter {
        private AlarmSettingDatabase.AlarmData[] mAlarmDataArray = null;
        private Thread mUpdateThread = null;

        public synchronized void update(){
            if(mUpdateThread == null){
                mUpdateThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mAlarmDataArray = (AlarmSettingDatabase.AlarmData[]) mAlarmSettingDatabase.getData(AlarmSettingDatabase.AlarmData.class);
                        notifyDataSetChanged();
                    }
                });
                mUpdateThread.start();
            }
        }

        @Override
        public int getCount() {
            return mAlarmDataArray == null ? 0 : mAlarmDataArray.length;
        }

        @Override
        public Object getItem(int i) {
            return mAlarmDataArray == null ? null : mAlarmDataArray[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Activity activity = getActivity();
            AlarmSettingDatabase.AlarmData item = (AlarmSettingDatabase.AlarmData) getItem(i);

            if(view == null){
                LayoutInflater inflater = activity.getLayoutInflater();
                view = inflater.inflate(R.layout.item_alarm_setting, viewGroup, false);
            }

            view.findViewById(R.id.btnAddAlarm).setVisibility(View.GONE);
            view.findViewById(R.id.btnRemoveAlarm).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btnRemoveAlarm).setOnClickListener(mBtnClickListener);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(item.timeMS);

            Spinner spnrHour = (Spinner)view.findViewById(R.id.spnrHour);
            Spinner spnrMinute = (Spinner)view.findViewById(R.id.spnrMinute);
            spnrHour.setAdapter(mHourTimeAdapter);
            spnrMinute.setAdapter(mMinuteTimeAdapter);
            spnrHour.setSelection(calendar.get(Calendar.HOUR_OF_DAY));
            spnrMinute.setSelection(calendar.get(Calendar.MINUTE));

            ((TextView)view.findViewById(R.id.etTitle)).setText(item.title);
            ((SwitchCompat)view.findViewById(R.id.swtRepeat)).setChecked(item.repeat);

            return view;
        }
    }

    private static class TimeAdapter extends BaseAdapter{
        enum TimeElementType {
            Hour(24),
            Minute(60),
            Second(60),
            ;

            int mCount;
            TimeElementType(int count){
                mCount = count;
            }
        }

        private Context mContext;
        private TimeElementType mElementType;

        public TimeAdapter(Context context, TimeElementType elementType){
            mContext = context;
            mElementType = elementType;
        }

        @Override
        public int getCount() {
            return mElementType.mCount;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view == null){
                view = new TextView(mContext);
                ((TextView)view).setTextAppearance(mContext, android.R.style.TextAppearance_Large);
            }
            ((TextView)view).setText(String.valueOf(i));

            return view;
        }
    }
}
