package jp.co.thcomp.unlimited_hand;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.ToastUtil;

public class TestUnlimitedHandActivity extends Activity {
    private UhAccessHelper mUHAccessHelper;
    private ConnectUHTask mConnectingUHTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test_unlimited_hand_layout);
        findViewById(R.id.btnVibrate).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate0).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate1).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate2).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate3).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate4).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate5).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate6).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnStimulate7).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnSharpnessUp).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnSharpnessDown).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnVoltageUp).setOnClickListener(mBtnClickListener);
        findViewById(R.id.btnVoltageDown).setOnClickListener(mBtnClickListener);

        mUHAccessHelper = UhAccessHelper.getInstance(this);

        SwitchCompat swtEnableUH = (SwitchCompat) findViewById(R.id.swtConnectUnlimitedHand);
        swtEnableUH.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    if (mConnectingUHTask == null) {
                        mConnectingUHTask = new ConnectUHTask();
                        mConnectingUHTask.execute();
                    }
                } else {
                    mUHAccessHelper.disconnect();
                }
            }
        });
    }

    private class ConnectUHTask extends AsyncTask<Void, Void, UhAccessHelper.ConnectResult> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(TestUnlimitedHandActivity.this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setTitle("connecting to Unlimited Hand");
            mProgressDialog.show();
        }

        @Override
        protected UhAccessHelper.ConnectResult doInBackground(Void... voids) {
            return mUHAccessHelper.connect();
        }

        @Override
        protected void onPostExecute(UhAccessHelper.ConnectResult connectResult) {
            int visibility = View.GONE;

            try {
                super.onPostExecute(connectResult);

                String toastMessage = "error is occurred!";
                int showInterval = Toast.LENGTH_LONG;

                switch (connectResult) {
                    case Found:
                        toastMessage = "Found to Unlimited Hand";
                        showInterval = Toast.LENGTH_SHORT;
                        visibility = View.VISIBLE;
                        break;
                    case ErrNoSupportBT:
                        toastMessage = "Does not support Bluetooth";
                        break;
                    case ErrNotFoundUnlimitedHand:
                        toastMessage = "Does not find Unlimited Hand";
                        break;
                    default:
                        break;
                }

                ToastUtil.showToast(TestUnlimitedHandActivity.this, toastMessage, showInterval);
            } finally {
                findViewById(R.id.llActionButtonArea).setVisibility(visibility);
                mProgressDialog.dismiss();
                mConnectingUHTask = null;
            }
        }
    }

    private View.OnClickListener mBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

            switch (id){
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
