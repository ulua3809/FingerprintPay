package com.yyxx.wechatfp.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.yyxx.wechatfp.util.log.L;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ActivityViewObserver {

    private WeakReference<Activity> mActivityRef;
    private boolean mRunning = false;
    private String mViewIdentifier;

    public ActivityViewObserver(Activity weakRefActivity, String viewIdentifier) {
        this.mActivityRef = new WeakReference<>(weakRefActivity);
        this.mViewIdentifier = viewIdentifier;
    }

    public void start(long loopMSec, IActivityViewListener listener) {
        if (mRunning) {
            return;
        }
        mRunning = true;
        task(loopMSec, listener);
    }

    public void stop() {
        mRunning = false;
    }

    private void task(long loopMSec, IActivityViewListener listener) {
        if (!mRunning) {
            return;
        }
        Activity activity = mActivityRef.get();
        if (activity == null) {
            mRunning = false;
            return;
        }
        if (activity.isFinishing()) {
            mRunning = false;
            return;
        }
        if (activity.isDestroyed()) {
            mRunning = false;
            return;
        }

        List<View> viewList = new ArrayList<>();
        List<View> decorViewList = ViewUtil.getWindowManagerViews();
        for (View decorView : decorViewList) {
            if (decorView instanceof ViewGroup) {
            } else {
                continue;
            }
            ViewUtil.getChildViewsByType((ViewGroup) decorView, this.mViewIdentifier, viewList);
            if (viewList.size() > 0) {
                break;
            }
        }
        if (viewList.size() > 0) {
            for (View targetView : viewList) {
                if (ViewUtil.isViewVisibleInScreen(targetView.getRootView())) {
                    onViewFounded(listener, targetView);
                }
            }
        }
        Task.onMain(loopMSec, () -> task(loopMSec, listener));
    }

    private void onViewFounded(IActivityViewListener listener, View view) {
        try {
            listener.onViewFounded(this, view);
        } catch (Exception e) {
            L.e(e);
        }
    }

    public interface IActivityViewListener {
        void onViewFounded(ActivityViewObserver observer, View view);
    }
}
