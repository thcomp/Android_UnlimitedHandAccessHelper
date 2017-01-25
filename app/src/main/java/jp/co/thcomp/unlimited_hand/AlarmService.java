package jp.co.thcomp.unlimited_hand;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.TimeUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jp.co.thcomp.util.PreferenceUtil;

public class AlarmService extends Service {
    public AlarmService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if(action != null){
            switch (action){
                case Common.INTENT_ACTION_ALARM:
                    startAlarmActivity(intent);
                    break;
            }
        }

        return Service.START_STICKY_COMPATIBILITY;
    }

    private void startAlarmActivity(Intent intent){
        long currentTimeMS = System.currentTimeMillis();
        long savedAlarmTimeMS = PreferenceUtil.readPrefLong(this, Common.PREF_LONG_ALARM_TIME_MS, 0);

        if(Math.abs(currentTimeMS - savedAlarmTimeMS) < 60 * 1000){

        }
    }
}
