package com.surcumference.fingerprint.plugin.impl.unionpay;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_UNIONPAY;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.plugin.inf.IMockCurrentUser;
import com.surcumference.fingerprint.plugin.inf.OnFingerprintVerificationOKListener;
import com.surcumference.fingerprint.util.ActivityViewObserver;
import com.surcumference.fingerprint.util.ActivityViewObserverHolder;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BizBiometricIdentify;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.XBiometricIdentify;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.AlipayPayView;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import java.util.Map;
import java.util.WeakHashMap;

public class UnionPayBasePlugin implements IAppPlugin, IMockCurrentUser {

    private AlertDialog mFingerPrintAlertDialog;
    private WeakHashMap<View, View.OnAttachStateChangeListener> mView2OnAttachStateChangeListenerMap = new WeakHashMap<>();
    protected boolean mMockCurrentUser = false;
    protected XBiometricIdentify mFingerprintIdentify;

    private int mWeChatVersionCode = 0;

    private Map<Activity, Boolean> mActivityResumedMap = new WeakHashMap<>();

    @Override
    public int getVersionCode(Context context) {
        if (mWeChatVersionCode != 0) {
            return mWeChatVersionCode;
        }
        mWeChatVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_UNIONPAY);
        return mWeChatVersionCode;
    }

    protected synchronized void initFingerPrintLock(Context context,
                                                    AlertDialog dialog, String passwordEncrypted,
                                                    OnFingerprintVerificationOKListener onSuccessUnlockCallback) {
        L.d("指纹识别开始");
        mFingerprintIdentify = new BizBiometricIdentify(context)
                .withMockCurrentUserCallback(this)
                .decryptPasscode(passwordEncrypted, new BizBiometricIdentify.IdentifyListener() {
                    @Override
                    public void onInited(BizBiometricIdentify identify) {
                        super.onInited(identify);
                        if (identify.isUsingBiometricApi()) {
                            ViewUtils.setAlpha(dialog, 0);
                            ViewUtils.setDimAmount(dialog, 0);
                        } else {
                            ViewUtils.setAlpha(dialog, 1);
                            ViewUtils.setDimAmount(dialog, 0.6f);
                        }
                    }

                    @Override
                    public void onDecryptionSuccess(BizBiometricIdentify identify, @NonNull String decryptedContent) {
                        super.onDecryptionSuccess(identify, decryptedContent);
                        onSuccessUnlockCallback.onFingerprintVerificationOK(decryptedContent);
                    }

                    @Override
                    public void onFailed(BizBiometricIdentify target, FingerprintIdentifyFailInfo failInfo) {
                        super.onFailed(target, failInfo);
                        if (dialog != null) {
                            ViewUtils.setAlpha(dialog, 1);
                            ViewUtils.setDimAmount(dialog, 0.6f);
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }
                    }
                });
    }

    public synchronized void showFingerPrintDialog(@Nullable Activity activity, View targetView) {
        View rootView = targetView.getRootView();
        final Context context = rootView.getContext();
        final Config config = Config.from(context);
        try {
            if ("true".equals(rootView.getTag(R.id.unionpay_payview_shown))) {
                L.d("payview already shown, skip.");
                return;
            }
            ViewTreeObserver.OnGlobalLayoutListener paymentMethodListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View selectPaymentMethodView = ViewUtils.findViewByText(rootView, "选择付款方式");
                    L.d("selectPaymentMethodView", ViewUtils.getViewInfo(selectPaymentMethodView));
                    if (selectPaymentMethodView == null) {
                        showFingerPrintDialog(activity, targetView);
                    }
                }
            };

            //监视选择付款页面, 并保证只有一个listener
            ViewTreeObserver.OnGlobalLayoutListener lastPaymentMethodListener = (ViewTreeObserver.OnGlobalLayoutListener)targetView.getTag(R.id.unionpay_payment_method_listener);
            if (lastPaymentMethodListener != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    targetView.getViewTreeObserver().removeOnGlobalLayoutListener(lastPaymentMethodListener);
                }
            }
            targetView.getViewTreeObserver().addOnGlobalLayoutListener(paymentMethodListener);
            targetView.setTag(R.id.unionpay_payment_method_listener, paymentMethodListener);

            rootView.setTag(R.id.unionpay_payview_shown, "true");
            String passwordEncrypted = config.getPasswordEncrypted();
            if (TextUtils.isEmpty(passwordEncrypted) || TextUtils.isEmpty(config.getPasswordIV())) {
                Toaster.showLong(Lang.getString(R.id.toast_password_not_set_generic));
                rootView.setTag(R.id.unionpay_payview_shown, null);
                return;
            }
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
            boolean finalDialogMode = dialogMode;
            AlipayPayView payView = new AlipayPayView(context)
                    .withOnCancelButtonClickListener(target -> {
                AlertDialog dialog = target.getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (finalDialogMode) {
                    Task.onBackground(100, () -> {
                        Task.onMain(500, () -> {
                            //窗口消失了
                            if (ViewUtils.isViewVisibleInScreen(rootView) == false) {
                                return;
                            }
                            retryWatchPayViewIfPossible(activity, rootView, 10, 500);
                        });
                    });
                }
            }).withOnDismissListener(v -> {
                XBiometricIdentify fingerprintIdentify = mFingerprintIdentify;
                if (fingerprintIdentify != null) {
                    fingerprintIdentify.cancelIdentify();
                    L.d("指纹识别取消1");
                }
                rootView.setTag(R.id.unionpay_payview_shown, null);
            }).withOnShowListener(target -> {
                ViewUtils.setAlpha(target.getDialog(), 0);
                ViewUtils.setDimAmount(target.getDialog(), 0);
                //for hidePreviousPayDialog
                Task.onMain(250, () -> {
                    //窗口消失了
                    if (ViewUtils.isViewVisibleInScreen(rootView) == false) {
                        return;
                    }
                    ViewUtils.setAlpha(target.getDialog(), 1);
                    ViewUtils.setDimAmount(target.getDialog(), 0.6f);
                    initFingerPrintLock(context, target.getDialog(), passwordEncrypted, (password) -> {
                        BlackListUtils.applyIfNeeded(context);

                        Runnable onCompleteRunnable = () -> {
                            AlertDialog dialog = mFingerPrintAlertDialog;
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        };

                        boolean tryAgain = false;
                        try {
                            inputDigitPassword(rootView, password);
                        } catch (NullPointerException e) {
                            tryAgain = true;
                        } catch (Exception e) {
                            Toaster.showLong(Lang.getString(R.id.toast_password_auto_enter_fail));
                            L.e(e);
                        }
                        if (tryAgain) {
                            Task.onMain(1000, () -> {
                                try {
                                    inputDigitPassword(rootView, password);
                                } catch (NullPointerException e) {
                                    Toaster.showLong(Lang.getString(R.id.toast_password_auto_enter_fail));
                                    L.d("inputDigitPassword NPE", e);
                                } catch (Exception e) {
                                    Toaster.showLong(Lang.getString(R.id.toast_password_auto_enter_fail));
                                    L.e(e);
                                }
                                onCompleteRunnable.run();
                            });
                            return;
                        }
                        onCompleteRunnable.run();
                    });
                }
                );
            });
            L.d("ViewUtils.isViewVisibleInScreen(rootView) ",ViewUtils.isViewVisibleInScreen(rootView) );
            //窗口消失了
            if (ViewUtils.isViewVisibleInScreen(rootView) == false) {
                rootView.setTag(R.id.unionpay_payview_shown, null);
                return;
            }
            AlertDialog fingerPrintAlertDialog = payView.showInDialog();
            ViewUtils.setAlpha(fingerPrintAlertDialog, 0);
            ViewUtils.setDimAmount(fingerPrintAlertDialog, 0);
            mFingerPrintAlertDialog = fingerPrintAlertDialog;
        } catch (OutOfMemoryError e) {
        }
    }

    private void retryWatchPayViewIfPossible(Activity activity, View rootView, int countDown, int delayMsec) {
        View payTitleTextView = ViewUtils.findViewByText(rootView, "请输入支付密码");
        if (payTitleTextView == null
                || !(ViewUtils.isViewVisibleInScreen(payTitleTextView) && ViewUtils.isShownInScreen(payTitleTextView))) {
            watchPayView(activity);
        }
        if (countDown > 0) {
            Task.onMain(delayMsec, () -> retryWatchPayViewIfPossible(activity, rootView, countDown - 1, delayMsec));
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

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivityResumed(Activity activity) {
        L.d("Activity onResume =", activity);
        mActivityResumedMap.put(activity, true);
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

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivityPaused(Activity activity) {
        L.d("Activity onPause =", activity);
        mActivityResumedMap.remove(activity);
        final String activityClzName = activity.getClass().getName();
        if (activityClzName.contains(".UPActivityReactNative")) {
            hidePreviousPayDialog();
        } else if (activityClzName.contains(".PayWalletActivity")) {
            hidePreviousPayDialog();
        }
        ActivityViewObserverHolder.stop(ActivityViewObserverHolder.Key.UnionPayPasswordView);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    @Override
    public boolean getMockCurrentUser() {
        return this.mMockCurrentUser;
    }


    @Override
    public void setMockCurrentUser(boolean mock) {
        this.mMockCurrentUser = mock;
    }

    private void watchPayView(Activity activity) {
        ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
        activityViewObserver.setViewIdentifyText("请输入支付密码");
        activityViewObserver.setWatchActivityViewOnly(true);
        ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.UnionPayPasswordView,  activityViewObserver,
                100, new ActivityViewObserver.IActivityViewListener() {
            @Override
            public void onViewFounded(ActivityViewObserver observer, View view) {
                //跳过没有显示的view, 云闪付专属
                if (!ViewUtils.isViewVisibleInScreen(view)) {
                    L.d("skip invisible view", ViewUtils.getViewInfo(view));
                    return;
                }
                if (!ViewUtils.isShownInScreen(view)) {
                    L.d("skip invisible view2", ViewUtils.getViewInfo(view));
                    return;
                }
                View rootView = view.getRootView();
                L.d("onViewFounded:", ViewUtils.getViewInfo(view), " rootView: ", rootView);
                if (rootView.toString().contains("[UPActivityPayPasswordSet]")) {
                    //跳过小额免密设置页面
                    return;
                }
                ActivityViewObserver.IActivityViewListener l = this;
                observer.stop();

                if (!mActivityResumedMap.containsKey(activity)) {
                    return;
                }

                onPayDialogShown(observer.getTargetActivity(), view);
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
    }

    protected void onPayDialogShown(@Nullable Activity activity, View targetView) {
        L.d("PayDialog show");
        showFingerPrintDialog(activity, targetView);
    }

    protected void onPayDialogDismiss(Context context) {
        L.d("PayDialog dismiss");
        if (Config.from(context).isOn()) {
            XBiometricIdentify fingerPrintIdentify = mFingerprintIdentify;
            if (fingerPrintIdentify != null) {
                fingerPrintIdentify.cancelIdentify();
                L.d("指纹识别取消2");
            }
            mMockCurrentUser = false;
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
