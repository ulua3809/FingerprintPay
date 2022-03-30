package com.surcumference.fingerprint.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;

import com.surcumference.fingerprint.util.log.L;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;

public class FragmentObserver {

    private WeakReference<Activity> mActivityRef;
    private boolean mRunning = false;
    private String mFragmentIdentifyClassName;
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    private long mLastTriggerTimeMillis = 0;
    private final long IGNORE_TIME_MILLIS = 100;
    private Handler mTasksHandler = new Handler(Looper.getMainLooper());
    private IFragmentViewListener mIFragmentViewListener;

    public FragmentObserver(Activity weakRefActivity) {
        this.mActivityRef = new WeakReference<>(weakRefActivity);
    }

    public void setFragmentIdentifyClassName(String fragmentIdentifyClassName) {
        this.mFragmentIdentifyClassName = fragmentIdentifyClassName;
    }

    public void start(IFragmentViewListener listener) {
        if (TextUtils.isEmpty(this.mFragmentIdentifyClassName)) {
            throw new IllegalArgumentException("Error: FragmentIdentifyClassName not set");
        }
        mIFragmentViewListener = listener;
        mRunning = true;
        Activity activity = mActivityRef.get();
        if (activity == null) {
            mRunning = false;
            return;
        }
        if ("androidx.appcompat.app.AppCompatActivity".equals(activity.getClass().getName())) {
            L.e("Target activity", activity, "is not androidx.appcompat.app.AppCompatActivity");
            mRunning = false;
            return;
        }
        ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
        if (mOnGlobalLayoutListener != null) {
            viewTreeObserver.removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
        mOnGlobalLayoutListener = () -> {
            long currentTime = SystemClock.uptimeMillis();
            if (mLastTriggerTimeMillis != 0 && currentTime - mLastTriggerTimeMillis <= IGNORE_TIME_MILLIS) {
                mTasksHandler.removeCallbacks(mOnGlobalLayoutRunnable);
                mTasksHandler.postDelayed(mOnGlobalLayoutRunnable, IGNORE_TIME_MILLIS);
                return;
            }
            mLastTriggerTimeMillis = currentTime;
            mOnGlobalLayoutRunnable.run();
        };
        viewTreeObserver.addOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    public void stop() {
        mRunning = false;
        Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }
        if (mOnGlobalLayoutListener != null) {
            ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
            viewTreeObserver.removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
            mOnGlobalLayoutListener = null;
        }
    }
    private Runnable mOnGlobalLayoutRunnable = () -> {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }
        try {
            Method getSupportFragmentManagerMethod = ReflectionUtils.findMethod(activity, "getSupportFragmentManager");
            Object getSupportFragmentManagerObject = getSupportFragmentManagerMethod.invoke(activity);
            Method getFragmentsMethod = ReflectionUtils.findMethod(getSupportFragmentManagerObject, "getFragments");
            List<Object> list = (List<Object>) getFragmentsMethod.invoke(getSupportFragmentManagerObject);
            Object currentFragmentObject = list.size() > 0 ? list.get(list.size() - 1) : null;
            if (currentFragmentObject.getClass().getName().contains(this.mFragmentIdentifyClassName)) {
                Method getViewMethod = ReflectionUtils.findMethod(currentFragmentObject, "getView");
                View fragmentViewObject = (View)getViewMethod.invoke(currentFragmentObject);
                onFragmentFounded(this.mIFragmentViewListener, currentFragmentObject, fragmentViewObject);
            }
        } catch (Exception e) {
            L.e(e);
        }
    };

    @Nullable
    public Activity getTargetActivity() {
        return mActivityRef.get();
    }

    private void onFragmentFounded(@Nullable IFragmentViewListener listener, Object fragmentObject, View fragmentRootView) {
        if (listener == null) {
            return;
        }
        try {
            listener.onFragmentFounded(this, fragmentObject, fragmentRootView);
        } catch (Exception e) {
            L.e(e);
        }
    }

    public interface IFragmentViewListener {
        void onFragmentFounded(FragmentObserver observer, Object fragmentObject, View fragmentRootView);
    }
}
