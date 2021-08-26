package com.surcumference.fingerprint.util;

import android.view.View;

import com.surcumference.fingerprint.util.log.L;

import java.lang.ref.WeakReference;

public class ViewVisibleInScreenObserver {

    private WeakReference<View> mViewRef;
    private boolean mViewVisibleInScreen;
    private boolean mRunning = false;

    public ViewVisibleInScreenObserver(View weakRefView) {
        this.mViewRef = new WeakReference<>(weakRefView);
        this.mViewVisibleInScreen = ViewUtils.isViewVisibleInScreen(weakRefView);
    }

    public void start(long loopMSec, IViewInScreenListener listener) {
        if (mRunning) {
            return;
        }
        mRunning = true;
        task(loopMSec, listener);
    }

    public void stop() {
        mRunning = false;
    }

    private void task(long loopMSec, IViewInScreenListener listener) {
        if (!mRunning) {
            return;
        }
        View view = mViewRef.get();
        if (view == null) {
            onViewVisbileInScreenStateChanged(listener, null, false);
            mRunning = false;
            return;
        }

        boolean visibleInScreen = ViewUtils.isViewVisibleInScreen(view);
        L.d("visibleInScreen", visibleInScreen);
        if (visibleInScreen == mViewVisibleInScreen) {
            Task.onMain(loopMSec, () -> task(loopMSec, listener));
            return;
        }
        mViewVisibleInScreen = visibleInScreen;
        onViewVisbileInScreenStateChanged(listener, view, visibleInScreen);
        Task.onMain(loopMSec, () -> task(loopMSec, listener));
    }

    private void onViewVisbileInScreenStateChanged(IViewInScreenListener listener, View view, boolean visibleInScreen) {
        try {
            listener.onViewVisibleInScreenStateChanged(this, view, visibleInScreen);
        } catch (Exception e) {
            L.e(e);
        }
    }

    public interface IViewInScreenListener {
        void onViewVisibleInScreenStateChanged(ViewVisibleInScreenObserver observer, View view, boolean visibleInScreen);
    }

}
