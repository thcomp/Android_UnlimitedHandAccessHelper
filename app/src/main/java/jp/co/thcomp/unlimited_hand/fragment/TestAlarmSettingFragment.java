package jp.co.thcomp.unlimited_hand.fragment;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

import jp.co.thcomp.unlimited_hand.AlarmService;
import jp.co.thcomp.unlimited_hand.Common;
import jp.co.thcomp.unlimited_hand.R;

public class TestAlarmSettingFragment extends AbstractTestFragment {
    private Spinner mSpnrHour;
    private Spinner mSpnrMinute;
    private CheckBox mCbEnableAlarm;

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
        TimeAdapter hourTimeAdapter = new TimeAdapter(activity, TimeAdapter.TimeElementType.Hour);
        TimeAdapter minuteTimeAdapter = new TimeAdapter(activity, TimeAdapter.TimeElementType.Minute);
        mSpnrHour = (Spinner) mRootView.findViewById(R.id.spnrHour);
        mSpnrMinute = (Spinner) mRootView.findViewById(R.id.spnrMinute);
        mCbEnableAlarm = (CheckBox) mRootView.findViewById(R.id.cbEnableAlarm);
        mSpnrHour.setAdapter(hourTimeAdapter);
        mSpnrMinute.setAdapter(minuteTimeAdapter);
        mRootView.findViewById(R.id.btnSaveAlarm).setOnClickListener(mBtnClickListener);

        return mRootView;
    }

    private void startAlarm() {
        Context context = getContext();
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(Common.INTENT_ACTION_ALARM);
        alarmIntent.setClass(context, AlarmService.class);
        PendingIntent alarmPendingIntent = PendingIntent.getService(context, 0, alarmIntent, 0);

        long currentTimeMS = System.currentTimeMillis();
        Calendar alarmFireCalendar = Calendar.getInstance();
        alarmFireCalendar.set(Calendar.HOUR_OF_DAY, (int)mSpnrHour.getSelectedItem());
        alarmFireCalendar.set(Calendar.MINUTE, (int)mSpnrMinute.getSelectedItem());

        if(currentTimeMS > alarmFireCalendar.getTimeInMillis()){
            // alarm time is past, set next day
            alarmFireCalendar.set(Calendar.DAY_OF_MONTH, alarmFireCalendar.get(Calendar.DAY_OF_MONTH) + 1);
        }

        // cancel previous alarm at once
        manager.cancel(alarmPendingIntent);
        manager.set(AlarmManager.RTC_WAKEUP, alarmFireCalendar.getTimeInMillis(), alarmPendingIntent);
    }

    private void saveAlarmInfo() {

    }

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

            switch (id) {
                case R.id.btnSaveAlarm:
                    if (mCbEnableAlarm.isChecked()) {
                        startAlarm();
                    }
                    saveAlarmInfo();
                    break;
            }
        }
    };

    private static class TimeAdapter extends BaseAdapter {
        enum TimeElementType {
            Hour(24),
            Minute(60),
            Second(60),;

            int mCount;

            TimeElementType(int count) {
                mCount = count;
            }
        }

        private Context mContext;
        private TimeElementType mElementType;

        public TimeAdapter(Context context, TimeElementType elementType) {
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
            if (view == null) {
                view = new TextView(mContext);
                ((TextView) view).setTextAppearance(mContext, android.R.style.TextAppearance_Large);
            }
            TextView tvView = ((TextView) view);
            tvView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimensionPixelSize(R.dimen.alarm_display_text_size));
            tvView.setText(String.valueOf(i));

            return view;
        }
    }
}
