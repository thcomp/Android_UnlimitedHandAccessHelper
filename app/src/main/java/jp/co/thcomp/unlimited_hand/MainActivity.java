package jp.co.thcomp.unlimited_hand;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import jp.co.thcomp.unlimited_hand.fragment.TestBatchOutputFragment;
import jp.co.thcomp.unlimited_hand.fragment.TestInputFragment;
import jp.co.thcomp.unlimited_hand.fragment.TestOutputFragment;
import jp.co.thcomp.unlimitedhand.UhAccessHelper;
import jp.co.thcomp.util.ToastUtil;

public class MainActivity extends AppCompatActivity {
    private static final MenuItem[] MENU_ITEMS = {
            new MenuItem("output", TestOutputFragment.class),
            new MenuItem("batch output", TestBatchOutputFragment.class),
            new MenuItem("input", TestInputFragment.class),
    };

    private UhAccessHelper mUHAccessHelper = null;
    private ConnectUHTask mConnectingUHTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUHAccessHelper = UhAccessHelper.getInstance(this);

        SwitchCompat swtConnectUHAccess = (SwitchCompat) findViewById(R.id.swtConnectUnlimitedHand);
        swtConnectUHAccess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    if (mConnectingUHTask == null) {
                        mConnectingUHTask = new ConnectUHTask();
                        mConnectingUHTask.execute();
                    }
                } else {
                    mUHAccessHelper.disconnect();
                    findViewById(R.id.llContentArea).setVisibility(View.GONE);
                }
            }
        });

        Spinner menuSpinner = (Spinner) findViewById(R.id.spnrMenu);
        menuSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MenuItem menuItem = (MenuItem) adapterView.getItemAtPosition(i);
                Class targetFragmentClass = menuItem.targetFragmentClass;
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment targetFragment = null;

                List<Fragment> fragmentList = fragmentManager.getFragments();
                if ((fragmentList != null) && (fragmentList.size() > 0)) {
                    for (Fragment fragment : fragmentList) {
                        if ((fragment != null) && (fragment.getClass() == targetFragmentClass)) {
                            targetFragment = fragment;
                            break;
                        }
                    }
                }

                if (targetFragment == null) {
                    Method newInstanceMethod = null;
                    String errorMessage = null;

                    try {
                        newInstanceMethod = targetFragmentClass.getMethod("newInstance");
                        targetFragment = (Fragment) newInstanceMethod.invoke(null);
                    } catch (NoSuchMethodException e) {
                        errorMessage = e.getLocalizedMessage();
                    } catch (InvocationTargetException e) {
                        errorMessage = e.getLocalizedMessage();
                    } catch (IllegalAccessException e) {
                        errorMessage = e.getLocalizedMessage();
                    }

                    if (errorMessage != null) {
                        ToastUtil.showToast(MainActivity.this, errorMessage, Toast.LENGTH_SHORT);
                    }
                }

                if (targetFragment != null) {
                    fragmentManager.beginTransaction().replace(R.id.flContentArea, targetFragment, targetFragmentClass.getSimpleName()).commitAllowingStateLoss();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // 処理なし
            }
        });
        menuSpinner.setAdapter(new MenuAdapter(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUHAccessHelper.disconnect();
    }

    private static class MenuItem {
        public String menuTitle;
        public Class targetFragmentClass;

        public MenuItem(String menuTitle, Class targetFragmentClass) {
            this.menuTitle = menuTitle;
            this.targetFragmentClass = targetFragmentClass;
        }
    }

    private static class MenuAdapter extends BaseAdapter {
        private Context mContext;

        public MenuAdapter(Context context) {
            if (context == null) {
                throw new NullPointerException("context == null");
            }

            mContext = context;
        }

        @Override
        public int getCount() {
            return MENU_ITEMS.length;
        }

        @Override
        public Object getItem(int i) {
            return MENU_ITEMS[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MenuItem menuItem = (MenuItem) getItem(i);
            View ret = view;

            if (ret == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ret = inflater.inflate(R.layout.item_content_menu, viewGroup, false);
            }

            ((TextView) ret.findViewById(R.id.tvMenuTitle)).setText(menuItem.menuTitle);

            return ret;
        }
    }

    private class ConnectUHTask extends AsyncTask<Void, Void, UhAccessHelper.ConnectResult> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setTitle("preparing Unlimited Hand helper");
            mProgressDialog.setCanceledOnTouchOutside(false);
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
                    case Connected:
                        toastMessage = "Already connected Unlimited Hand";
                        showInterval = Toast.LENGTH_SHORT;
                        visibility = View.VISIBLE;
                        break;
                    case ErrNoSupportBT:
                        toastMessage = "Not support Bluetooth";
                        break;
                    case ErrNotPairedUnlimitedHand:
                        toastMessage = "Not paired Unlimited Hand";
                        break;
                    case PairedWithoutConnection:
                        toastMessage = "Paired Unlimited Hand (without connection)";
                        showInterval = Toast.LENGTH_SHORT;
                        visibility = View.VISIBLE;
                        break;
                    default:
                        break;
                }

                ToastUtil.showToast(MainActivity.this, toastMessage, showInterval);
            } finally {
                findViewById(R.id.llContentArea).setVisibility(visibility);
                mProgressDialog.dismiss();
                mConnectingUHTask = null;
            }
        }
    }
}
