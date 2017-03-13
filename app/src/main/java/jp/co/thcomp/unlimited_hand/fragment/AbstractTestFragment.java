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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUHAccessHelper = ((UHAccessHelperProvider)getActivity()).getUhAccessHelper();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mRootView = inflater.inflate(getLayoutResId(), container, false);
        return mRootView;
    }

    public interface UHAccessHelperProvider {
        public UhAccessHelper getUhAccessHelper();
    }
}
