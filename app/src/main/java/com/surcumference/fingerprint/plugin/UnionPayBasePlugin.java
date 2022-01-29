package com.surcumference.fingerprint.plugin;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_UNIONPAY;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.ActivityViewObserver;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.NotifyUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.AlipayPayView;
import com.surcumference.fingerprint.view.DialogFrameLayout;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;

import java.util.WeakHashMap;

public class UnionPayBasePlugin {

    private AlertDialog mFingerPrintAlertDialog;
    private boolean mPwdActivityDontShowFlag;
    private int mPwdActivityReShowDelayTimeMsec;

    private ActivityViewObserver mActivityViewObserver;
    private WeakHashMap<View, View.OnAttachStateChangeListener> mView2OnAttachStateChangeListenerMap = new WeakHashMap<>();
    protected boolean mMockCurrentUser = false;
    protected FingerprintIdentify mFingerprintIdentify;

    private int mWeChatVersionCode = 0;

    private int getUnionPayVersionCode(Context context) {
        if (mWeChatVersionCode != 0) {
            return mWeChatVersionCode;
        }
        mWeChatVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_UNIONPAY);
        return mWeChatVersionCode;
    }

    protected synchronized void initFingerPrintLock(Context context, Runnable onSuccessUnlockRunnable) {
        mMockCurrentUser = true;
        mFingerprintIdentify = new FingerprintIdentify(context.getApplicationContext(), exception -> {
            if (exception instanceof SsdkUnsupportedException) {
                return;
            }
            L.e("fingerprint", exception);
        });
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(5, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_match));
                    L.d("指纹识别成功");
                    onSuccessUnlockRunnable.run();
                    mMockCurrentUser = false;
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    L.d("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_match));
                    mMockCurrentUser = false;
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    L.d("多次尝试错误，请确认指纹 isDeviceLocked", isDeviceLocked);
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_retry_ended));
                    mMockCurrentUser = false;
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_unlock_reboot));
                    mMockCurrentUser = false;
                }
            });
        } else {
            L.d("系统指纹功能未启用");
            NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_enable));
            mMockCurrentUser = false;
        }
    }

    public void showFingerPrintDialog(@Nullable Activity activity, View rootView) {
        final Context context = rootView.getContext();
        try {
            hidePreviousPayDialog();
            boolean dialogMode = false;
            View payRootLayout = ViewUtils.findViewByName(rootView, PACKAGE_NAME_UNIONPAY, "fl_container");
            L.d("payRootLayout", payRootLayout);
            if (payRootLayout == null) {
                View payTitleTextView = ViewUtils.findViewByText(rootView, "请输入支付密码");
                payRootLayout = (View)payTitleTextView.getParent().getParent().getParent();
                dialogMode = true;
            }
            L.d("payRootLayout", payRootLayout);
            View finalPayRootLayout = payRootLayout;
            payRootLayout.setAlpha(0);
            //for hidePreviousPayDialog
            Task.onMain(100, () -> finalPayRootLayout.setAlpha(0));

            mPwdActivityDontShowFlag = false;
            mPwdActivityReShowDelayTimeMsec = 0;
            //for hidePreviousPayDialog
            Task.onMain(250, () ->
                initFingerPrintLock(context, () -> {
                    BlackListUtils.applyIfNeeded(context);
                    String pwd = Config.from(context).getPassword();
                    if (TextUtils.isEmpty(pwd)) {
                        Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_alipay), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Runnable onCompleteRunnable = () -> {
                        mPwdActivityReShowDelayTimeMsec = 2000;
                        AlertDialog dialog = mFingerPrintAlertDialog;
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    };

                    boolean tryAgain = false;
                    try {
                        inputDigitPassword(rootView, pwd);
                    } catch (NullPointerException e) {
                        tryAgain = true;
                    } catch (Exception e) {
                        Toast.makeText(context, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                        L.e(e);
                    }
                    if (tryAgain) {
                        Task.onMain(1000, ()-> {
                            try {
                                inputDigitPassword(rootView, pwd);
                            } catch (NullPointerException e) {
                                Toast.makeText(context, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                                L.d("inputDigitPassword NPE", e);
                            } catch (Exception e) {
                                Toast.makeText(context, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                                L.e(e);
                            }
                            onCompleteRunnable.run();
                        });
                        return;
                    }
                    onCompleteRunnable.run();
                })
            );
            boolean finalDialogMode = dialogMode;
            DialogFrameLayout payView = new AlipayPayView(context).withOnCloseImageClickListener(v -> {
                mPwdActivityDontShowFlag = true;
                AlertDialog dialog1 = mFingerPrintAlertDialog;
                if (dialog1 != null) {
                    dialog1.dismiss();
                }

                if (finalDialogMode) {
                    Task.onBackground(() -> {
                        Instrumentation inst = new Instrumentation();
                        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                    });
                } else {
                    if (activity != null) {
                        activity.onBackPressed();
                    }
                    Task.onMain(300, () -> finalPayRootLayout.setAlpha(1));
                }
            }).withOnDismissListener(v -> {
                FingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
                if (fingerprintIdentify != null) {
                    fingerprintIdentify.cancelIdentify();
                }
                if (!mPwdActivityDontShowFlag) {
                    Task.onMain(mPwdActivityReShowDelayTimeMsec, () -> finalPayRootLayout.setAlpha(1));
                }
            });
            Task.onMain(100,  () -> mFingerPrintAlertDialog = payView.showInDialog());
        } catch (OutOfMemoryError e) {
        }
    }

    private void inputDigitPassword(View rootView, String password) {
        View ks[] = new View[] {
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "1"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "2"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "3"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "4"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "5"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "6"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "7"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "8"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "9"),
                ViewUtils.findViewByText(rootView, PACKAGE_NAME_UNIONPAY, "0"),
        };
        char[] chars = password.toCharArray();
        for (char c : chars) {
            View v;
            switch (c) {
                case '1':
                    v = ks[0];
                    break;
                case '2':
                    v = ks[1];
                    break;
                case '3':
                    v = ks[2];
                    break;
                case '4':
                    v = ks[3];
                    break;
                case '5':
                    v = ks[4];
                    break;
                case '6':
                    v = ks[5];
                    break;
                case '7':
                    v = ks[6];
                    break;
                case '8':
                    v = ks[7];
                    break;
                case '9':
                    v = ks[8];
                    break;
                case '0':
                    v = ks[9];
                    break;
                default:
                    continue;
            }
            ViewUtils.performActionClick(v);
        }
    }

    protected void onActivityResumed(Activity activity) {
        L.d("Activity onResume =", activity);
        final String activityClzName = activity.getClass().getName();
        if (activityClzName.contains(".UPActivityReactNative")) {

            ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
            activityViewObserver.setViewIdentifyText("通用设置");
            activityViewObserver.start(100, (observer, view) -> doSettingsMenuInject(activity));
            Task.onBackground(2000, activityViewObserver::stop);
            watchPayView(activity);
        } else if (activityClzName.contains(".PayWalletActivity")) {
            watchPayView(activity);
        }
    }

    public void onActivityPaused(Activity activity) {
        L.d("Activity onPause =", activity);
        final String activityClzName = activity.getClass().getName();
        if (activityClzName.contains(".UPActivityReactNative")) {
            hidePreviousPayDialog();
        } else if (activityClzName.contains(".PayWalletActivity")) {
            hidePreviousPayDialog();
        }
    }

    private void watchPayView(Activity activity) {
        stopAndRemoveCurrentActivityViewObserver();
        ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
        activityViewObserver.setViewIdentifyText("请输入支付密码");
        activityViewObserver.start(100, new ActivityViewObserver.IActivityViewListener() {
            @Override
            public void onViewFounded(ActivityViewObserver observer, View view) {
                View rootView = view.getRootView();
                L.d("onViewFounded:", view, " rootView: ", rootView);
                if (rootView.toString().contains("[UPActivityPayPasswordSet]")) {
                    //跳过小额免密设置页面
                    return;
                }
                ActivityViewObserver.IActivityViewListener l = this;
                observer.stop();

                onPayDialogShown(observer.getTargetActivity(), (ViewGroup) rootView);
                View.OnAttachStateChangeListener listener = mView2OnAttachStateChangeListenerMap.get(view);
                if (listener != null) {
                    view.removeOnAttachStateChangeListener(listener);
                }
                listener = new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        L.d("onViewAttachedToWindow:", view);

                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        L.d("onViewDetachedFromWindow:", view);
                        Context context = v.getContext();
                        onPayDialogDismiss(context);
                        Task.onMain(500, () -> observer.start(100, l));
                    }
                };
                view.addOnAttachStateChangeListener(listener);
                mView2OnAttachStateChangeListenerMap.put(view, listener);
            }
        });
        mActivityViewObserver = activityViewObserver;
    }

    protected void onPayDialogShown(@Nullable Activity activity, ViewGroup rootView) {
        L.d("PayDialog show");
        showFingerPrintDialog(activity, rootView);
    }

    protected void onPayDialogDismiss(Context context) {
        L.d("PayDialog dismiss");
        if (Config.from(context).isOn()) {
            FingerprintIdentify fingerPrintIdentify = mFingerprintIdentify;
            if (fingerPrintIdentify != null) {
                fingerPrintIdentify.cancelIdentify();
            }
            mMockCurrentUser = false;
        }
    }

    protected void stopAndRemoveCurrentActivityViewObserver() {
        ActivityViewObserver activityViewObserver = mActivityViewObserver;
        if (activityViewObserver != null) {
            activityViewObserver.stop();
            mActivityViewObserver = null;
        }
    }

    protected void doSettingsMenuInject(final Activity activity) {

        View rootView = activity.getWindow().getDecorView();
        if (ViewUtils.findViewByText(rootView, Lang.getString(R.id.app_settings_name)) != null) {
            return;
        }
        View genericSettingsView = ViewUtils.findViewByText(rootView, "通用设置");
        if (genericSettingsView == null) {
            return;
        }
        View paySettingsView = ViewUtils.findViewByText(rootView, "支付设置");
        if (paySettingsView == null) {
            return;
        }
        ViewGroup upReactView = ViewUtils.findParentViewByClassNamePart(paySettingsView, ".UPReactView");

        View paySettingsParentView = (View)paySettingsView.getParent().getParent().getParent();
        L.d("FragmentActivity", activity instanceof FragmentActivity);
        L.d("paySettingsView", paySettingsView);
        L.d("paySettingsParentView", paySettingsParentView);
        L.d("upReactView", upReactView);
        int appendHeight = 70;
        int childCount = ((ViewGroup) paySettingsParentView.getParent()).getChildCount();
        L.d("((ViewGroup)paySettingsParentView.getParent()).getChildCount()", childCount);
        for (int i = 0; i < childCount; i++) {
            View view = ((ViewGroup) paySettingsParentView.getParent()).getChildAt(i);
            view.setTop(view.getTop() + DpUtils.dip2px(activity, appendHeight));
            view.setBottom(view.getBottom() + DpUtils.dip2px(activity, appendHeight));
        }
        ((View)paySettingsParentView.getParent()).setBottom(((View)paySettingsParentView.getParent()).getBottom() + DpUtils.dip2px(activity, appendHeight));
        boolean isDarkMode = false;

        FrameLayout settingsItemRootContainer = new FrameLayout(activity);
        LinearLayout settingsItemRootLLayout = new LinearLayout(activity);
        settingsItemRootLLayout.setOrientation(LinearLayout.VERTICAL);
        settingsItemRootLLayout.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        settingsItemRootLLayout.setPadding(0, DpUtils.dip2px(activity, 20), 0, 0);

        LinearLayout settingsItemLinearLayout = new LinearLayout(activity);
        settingsItemLinearLayout.setOrientation(LinearLayout.VERTICAL);

        settingsItemLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);

        itemHlinearLayout.setBackgroundColor(isDarkMode ? 0xFF191919 : Color.WHITE);
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        TextView itemNameText = new TextView(activity);
        itemNameText.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_pressed},{-android.R.attr.state_pressed}},
                new int[]{0xFFD6D6D6, isDarkMode ? 0xFFD3D3D3 : 0xFF353535}));
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtils.dip2px(activity, 15), 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, DpUtils.dip2px(activity, 20), 0);
        itemSummerText.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_pressed},{-android.R.attr.state_pressed}},
                new int[]{0xFFD6D6D6, isDarkMode ? 0xFF656565 : 0xFF999999}));

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 55)));

        settingsItemRootLLayout.addView(settingsItemLinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        settingsItemRootLLayout.setTag(BuildConfig.APPLICATION_ID);

        settingsItemRootContainer.addView(settingsItemRootLLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // 滚动跟随
        ScrollView scrollView = ViewUtils.findParentViewByClass(paySettingsView, ScrollView.class);
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> settingsItemRootLLayout.setTop(-scrollY));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = DpUtils.dip2px(activity, 45);
        upReactView.addView(settingsItemRootContainer, params);

        // 切换至关爱版
        rootView.getViewTreeObserver().addOnDrawListener(() -> {
            int outLocation[] = new int[2];
            scrollView.getLocationInWindow(outLocation);
            settingsItemRootLLayout.setLeft(outLocation[0]);
            boolean isGlobalVisible = ((View) genericSettingsView).getGlobalVisibleRect(new Rect());
            if (!isGlobalVisible) {
                if (settingsItemRootContainer.getVisibility() != View.GONE) {
                    settingsItemRootContainer.setVisibility(View.GONE);
                }
            } else {
                if (settingsItemRootContainer.getVisibility() != View.VISIBLE) {
                    settingsItemRootContainer.setVisibility(View.VISIBLE);
                }
            }
            settingsItemRootLLayout.setTop(-scrollView.getScrollY());
        });
    }

    private void hidePreviousPayDialog() {
        AlertDialog dialog = mFingerPrintAlertDialog;
        L.d("hidePreviousPayDialog", mFingerPrintAlertDialog);
        if (dialog != null) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException e) {
                //for java.lang.IllegalArgumentException: View=DecorView@4eafdfb[UPActivityPayPasswordSet] not attached to window manager
            }
        }
        mFingerPrintAlertDialog = null;
    }
}
