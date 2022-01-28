package com.surcumference.fingerprint.util;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.surcumference.fingerprint.util.log.L;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ActivityViewObserver {

    private WeakReference<Activity> mActivityRef;
    private boolean mRunning = false;
    private String mViewIdentifyType;
    private String mViewIdentifyText;

    public ActivityViewObserver(Activity weakRefActivity) {
        this.mActivityRef = new WeakReference<>(weakRefActivity);
    }

    public void setViewIdentifyType(String viewIdentifyType) {
        this.mViewIdentifyType = viewIdentifyType;
    }

    public void setViewIdentifyText(String viewIdentifyText) {
        this.mViewIdentifyText = viewIdentifyText;
    }

    public void start(long loopMSec, IActivityViewListener listener) {
        if (TextUtils.isEmpty(this.mViewIdentifyType) && TextUtils.isEmpty(this.mViewIdentifyText)) {
            throw new IllegalArgumentException("Error: ViewIdentifyType or ViewIdentifyText not set");
        }
        if (mRunning) {
            return;
        }
        mRunning = true;
        task(loopMSec, listener);
    }

    public void stop() {
        mRunning = false;
    }

    @Nullable
    public Activity getTargetActivity() {
        return mActivityRef.get();
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
        List<View> decorViewList = ViewUtils.getWindowManagerViews();
        for (View decorView : decorViewList) {
            if (decorView instanceof ViewGroup) {
            } else {
                continue;
            }
            String viewIdentifyType = this.mViewIdentifyType;
            if (!TextUtils.isEmpty(viewIdentifyType)) {
                ViewUtils.getChildViewsByType((ViewGroup) decorView, viewIdentifyType, viewList);
                if (viewList.size() > 0) {
                    break;
                }
            }
            String viewIdentifyText = this.mViewIdentifyText;
            if (!TextUtils.isEmpty(viewIdentifyText)) {
                ViewUtils.getChildViews((ViewGroup) decorView, viewIdentifyText, viewList);
                if (viewList.size() > 0) {
                    break;
                }
            }
        }
        if (viewList.size() > 0) {
            for (View targetView : viewList) {
                if (ViewUtils.isViewVisibleInScreen(targetView.getRootView())) {
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
