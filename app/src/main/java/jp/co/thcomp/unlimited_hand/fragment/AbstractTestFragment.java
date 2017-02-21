package jp.co.thcomp.unlimited_hand.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import jp.co.thcomp.unlimitedhand.UhAccessHelper;

public abstract class AbstractTestFragment extends Fragment {
    protected UhAccessHelper mUHAccessHelper;
    protected View mRootView;

    abstract int getLayoutResId();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mUHAccessHelper = ((UHAccessHelperProvider)getActivity()).getUhAccessHelper();
        mRootView = inflater.inflate(getLayoutResId(), container, false);
        return mRootView;
    }

    public interface UHAccessHelperProvider {
        public UhAccessHelper getUhAccessHelper();
    }
}