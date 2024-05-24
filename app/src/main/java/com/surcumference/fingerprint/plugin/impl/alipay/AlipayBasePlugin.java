package com.surcumference.fingerprint.plugin.impl.alipay;

import static com.surcumference.fingerprint.Constant.ICON_ALIPAY_SETTING_ENTRY_BASE64;
import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_ALIPAY;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.DigitPasswordKeyPadInfo;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.plugin.inf.OnFingerprintVerificationOKListener;
import com.surcumference.fingerprint.util.ActivityViewObserver;
import com.surcumference.fingerprint.util.ActivityViewObserverHolder;
import com.surcumference.fingerprint.util.AlipayVersionControl;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BizBiometricIdentify;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.ImageUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.XBiometricIdentify;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.AlipayPayView;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import java.util.ArrayList;
import java.util.List;

public class AlipayBasePlugin implements IAppPlugin {


    private AlertDialog mFingerPrintAlertDialog;
    private int mPwdActivityReShowDelayTimeMsec;

    private XBiometricIdentify mFingerprintIdentify;
    private Activity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mAlipayVersionCode;

    @Override
    public int getVersionCode(Context context) {
        if (mAlipayVersionCode != 0) {
            return mAlipayVersionCode;
        }
        mAlipayVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_ALIPAY);
        return mAlipayVersionCode;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        L.d("activity", activity);
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            int alipayVersionCode = getVersionCode(activity);
            if (alipayVersionCode >= 773 /** 10.3.80.9100 */ && activityClzName.contains(".FBAppWindowActivity")) {
                ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
                activityViewObserver.setViewIdentifyText("支付密码", "支付密碼", "Payment Password");
                ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.AlipaySettingPageEntered,
                        activityViewObserver, 100, (observer, view) -> doSettingsMenuInject_10_1_38(activity),
                        30000);
            } else if (activityClzName.contains(".MySettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject_10_1_38(activity));
            } else if (activityClzName.contains(".UserSettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject(activity));
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        //Xposed not hooked yet!
        L.d("onActivityPaused", activity);
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
        return false;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    public void onActivityResumed(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            mCurrentActivity = activity;
            if (activityClzName.contains(".PayPwdDialogActivity")
                    || activityClzName.contains(".MspContainerActivity")
                    || activityClzName.contains(".FlyBirdWindowActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                mIsViewTreeObserverFirst = true;
                int versionCode = getVersionCode(activity);
                View rootView = activity.getWindow().getDecorView();
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (mCurrentActivity == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        if (activity.isDestroyed()) {
                            return;
                        }
                    }
                    if (mCurrentActivity != activity) {
                        return;
                    }
                    if (versionCode >= 661 /** 10.3.10.8310 */) {
                        boolean isRechargePay = (ViewUtils.isShown(ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password"))
                                && ViewUtils.isShown(ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "keyboard_container")));

                        boolean isNormalPay = ViewUtils.isShown(ViewUtils.findViewByText(rootView,"请输入长密码", "請輸入長密碼", "Payment Password"))
                                || ViewUtils.isShown(ViewUtils.findViewByText(rootView,"密码共6位，已输入0位"));
                        if (isRechargePay || isNormalPay) {
                            if (mIsViewTreeObserverFirst) {
                                if (showFingerPrintDialog(activity)) {
                                    mIsViewTreeObserverFirst = false;
                                }
                            }
                            return;
                        }
                        return;
                    }
                    if (ViewUtils.findViewByName(activity, (versionCode >= 352 /** 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.safepaybase", "mini_linSimplePwdComponent") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.safepaysdk", "mini_linSimplePwdComponent") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password") == null ) {
                        return;
                    }
                    if (mIsViewTreeObserverFirst) {
                        mIsViewTreeObserverFirst = false;
                        showFingerPrintDialog(activity);
                    }
                });

            } else if (activityClzName.contains("PayPwdHalfActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                Task.onMain(1500, () -> {
                    int versionCode = getVersionCode(activity);
                    DigitPasswordKeyPadInfo digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
                    View key1View = ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1);
                    if (key1View != null) {
                        showFingerPrintDialog(activity);
                        return;
                    }

                    //try again
                    Task.onMain(2000, () -> showFingerPrintDialog(activity));
                });
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    public void initFingerPrintLock(final Context context ,AlertDialog dialog, String passwordEncrypted,
                                    OnFingerprintVerificationOKListener onSuccessUnlockCallback) {
        mFingerprintIdentify = new BizBiometricIdentify(context)
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

    public boolean showFingerPrintDialog(final Activity activity) {
        final Context context = activity;
        final Config config = Config.from(context);
        try {
            if (getVersionCode(activity) >= 224) {
                if (activity.getClass().getName().contains(".MspContainerActivity")) {
                    View payTextView = ViewUtils.findViewByText(activity.getWindow().getDecorView(),
                            "支付宝支付密码", "支付寶支付密碼", "Alipay Payment Password",
                            "请输入支付密码", "請輸入支付密碼", "Payment password");
                    L.d("payTextView", payTextView);
                    if (payTextView == null) {
                        return false;
                    }
                }
            }

            hidePreviousPayDialog();
            String passwordEncrypted = config.getPasswordEncrypted();
            if (TextUtils.isEmpty(passwordEncrypted) || TextUtils.isEmpty(config.getPasswordIV())) {
                Toaster.showLong(Lang.getString(R.id.toast_password_not_set_alipay));
                return true;
            }

            mPwdActivityReShowDelayTimeMsec = 0;
            clickDigitPasswordWidget(activity);
            reEnteredPayDialogSolution(activity);
            AlipayPayView alipayPayView = new AlipayPayView(context)
                .withOnShowListener((target) -> {
                    initFingerPrintLock(context, target.getDialog(), passwordEncrypted, (password) -> {
                        BlackListUtils.applyIfNeeded(context);
                        Runnable onCompleteRunnable = () -> {
                            mPwdActivityReShowDelayTimeMsec = 1000;
                            AlertDialog dialog = mFingerPrintAlertDialog;
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        };

                        if (!tryInputGenericPassword(activity, password)) {
                            boolean tryAgain = false;
                            try {
                                inputDigitPassword(activity, password);
                            } catch (NullPointerException e) {
                                tryAgain = true;
                            } catch (Exception e) {
                                Toaster.showLong(Lang.getString(R.id.toast_password_auto_enter_fail));
                                L.e(e);
                            }
                            if (tryAgain) {
                                clickDigitPasswordWidget(activity);
                                Task.onMain(1000, ()-> {
                                    try {
                                        inputDigitPassword(activity, password);
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
                        }
                        onCompleteRunnable.run();
                    });
            }).withOnCloseImageClickListener((target, v) -> {
                target.getDialog().dismiss();
                activity.onBackPressed();
            }).withOnDismissListener(v -> {
                XBiometricIdentify fingerprintIdentify = mFingerprintIdentify;
                if (fingerprintIdentify != null) {
                    fingerprintIdentify.cancelIdentify();
                }
            });
            AlertDialog fingerPrintAlertDialog = alipayPayView.showInDialog();
            ViewUtils.setAlpha(fingerPrintAlertDialog, 0);
            ViewUtils.setDimAmount(fingerPrintAlertDialog, 0);
            mFingerPrintAlertDialog = fingerPrintAlertDialog;
        } catch (OutOfMemoryError e) {
        }
        return true;
    }

    private void reEnteredPayDialogSolution(Activity activity) {
        int versionCode = getVersionCode(activity);
        if (versionCode < 1261 /** 10.5.96.8000 */) {
            return;
        }
        // 在10s内寻找密码框
        ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
        activityViewObserver.setActivityViewFinder(outViewList -> {
            EditText view = findPasswordEditText(activity);
            if (view != null) {
                outViewList.add(view);
            }
            View shortPwdView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "密码共6位，已输入0位");
            if (ViewUtils.isShown(shortPwdView)) {
                outViewList.add(shortPwdView);
            }
        });
        ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.AlipayPasswordView,
                activityViewObserver, 300, (observer, view) -> {
                    ActivityViewObserverHolder.stop(observer);
                    L.d("找到密码框", view);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            private boolean lastFocusState = true; //初始化默认给true
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                L.d("密码框", "onFocusChange", view, hasFocus);
                                try {
                                    // 如果失去焦点并且获得新焦点, 通常是切换支付方式, 尝试重新触发识别
                                    if (!lastFocusState && hasFocus) {
                                        AlertDialog dialog = mFingerPrintAlertDialog;
                                        if (dialog == null) {
                                            return;
                                        }
                                        if (!dialog.isShowing()) {
                                            dialog.show();
                                        }
                                    }
                                } finally {
                                    lastFocusState = hasFocus;
                                }

                            }

                        });
                    }
                },
                10000);
    }

    /**
     * 修复某个设备不自动弹出键盘
     * @param activity
     */
    private void clickDigitPasswordWidget(Activity activity) {
        int versionCode = getVersionCode(activity);
        View view = ViewUtils.findViewByName(activity, (versionCode >= 352 /** 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout");
        L.d("digit password widget", view);
        if (view == null) {
            return;
        }
        ViewUtils.performActionClick(view);
    }

    private void doSettingsMenuInject_10_1_38(final Activity activity) {

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFEEEEEE);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).pressedColor(0xFFD9D9D9).create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtils.dip2px(activity, 12), 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, DpUtils.dip2px(activity, 18), 0);
        itemSummerText.setTextColor(0xFF999999);
        int versionCode = getVersionCode(activity);

        //try use Alipay style
        try {
            View settingsView = versionCode >= 773 /** 10.3.80.9100 */
                    ? ViewUtils.findViewByText(activity.getWindow().getDecorView(), "支付密码", "支付密碼", "Payment Password")
                    : ViewUtils.findViewByName(activity, "com.alipay.mobile.antui", "item_left_text");
            L.d("settingsView", settingsView);
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        if (versionCode >= 773 /** 10.3.80.9100 */) {
        } else if (versionCode >= 661 /** 10.3.10.8310 */) {
            ImageView itemIconImageView = new ImageView(activity);
            itemIconImageView.setImageBitmap(ImageUtils.base64ToBitmap(ICON_ALIPAY_SETTING_ENTRY_BASE64));
            LinearLayout.LayoutParams itemIconImageViewLayoutParams = new LinearLayout.LayoutParams(DpUtils.dip2px(activity, 24), DpUtils.dip2px(activity, 24));
            itemIconImageViewLayoutParams.leftMargin = DpUtils.dip2px(activity, 12);
            itemHlinearLayout.addView(itemIconImageView, itemIconImageViewLayoutParams);
        }
        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFEEEEEE);

        LinearLayout rootLinearLayout = new LinearLayout(activity);
        rootLinearLayout.setOrientation(LinearLayout.VERTICAL);
        rootLinearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);

        if (versionCode >= 661 /** 10.3.10.8310 */) {
            lineTopView.setVisibility(View.INVISIBLE);
            itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE)
                    .pressedColor(0xFFEBEBEB).round(32).create());
            lineBottomView.setVisibility(View.INVISIBLE);
            lineParams.bottomMargin = DpUtils.dip2px(activity, 8);
            rootLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 50)));
        } else {
            rootLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 45)));
            lineParams.bottomMargin = DpUtils.dip2px(activity, 20);
        }

        rootLinearLayout.addView(lineBottomView, lineParams);

        if (versionCode >= 773 /** 10.3.80.9100 */) {
            View itemView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "支付密码", "支付密碼", "Payment Password");
            if (itemView != null) {
                ViewGroup itemViewGroup = (ViewGroup) itemView.getParent().getParent().getParent().getParent();
                L.d("生物支付item: " + ViewUtils.getViewInfo(itemViewGroup));
                itemViewGroup.setPadding(0, DpUtils.dip2px(activity, 62), 0, 0);
                itemViewGroup.setClipToPadding(false);
                FrameLayout.LayoutParams rootLinearLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rootLinearLayoutParams.topMargin = -DpUtils.dip2px(activity, 50);
                rootLinearLayoutParams.leftMargin = DpUtils.dip2px(activity, 12);
                rootLinearLayoutParams.rightMargin = DpUtils.dip2px(activity, 12);
                itemViewGroup.addView(rootLinearLayout, rootLinearLayoutParams);
            }
        } else {
            int listViewId = activity.getResources().getIdentifier("setting_list", "id", "com.alipay.android.phone.openplatform");
            ListView listView = activity.findViewById(listViewId);
            listView.addHeaderView(rootLinearLayout);
        }
    }

    private void doSettingsMenuInject(final Activity activity) {
        int logout_id = activity.getResources().getIdentifier("logout", "id", "com.alipay.android.phone.openplatform");

        View logoutView = activity.findViewById(logout_id);
        LinearLayout linearLayout = (LinearLayout) logoutView.getParent();
        linearLayout.setPadding(0, 0, 0, 0);
        List<ViewGroup.LayoutParams> childViewParamsList = new ArrayList<>();
        List<View> childViewList = new ArrayList<>();
        int childViewCount = linearLayout.getChildCount();
        for (int i = 0; i < childViewCount; i++) {
            View view = linearLayout.getChildAt(i);
            childViewList.add(view);
            childViewParamsList.add(view.getLayoutParams());
        }

        linearLayout.removeAllViews();

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFDFDFDF);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtils.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF888888);

        //try use Alipay style
        try {
            View settingsView = ViewUtils.findViewByName(activity, "com.alipay.mobile.ui", "title_bar_title");
            L.d("settingsView", settingsView);
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFDFDFDF);

        linearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        linearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 50)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtils.dip2px(activity, 20);
        linearLayout.addView(lineBottomView, lineParams);

        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    private void inputDigitPassword(Activity activity, String password) {
        int versionCode = getVersionCode(activity);
        DigitPasswordKeyPadInfo digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
        View ks[] = new View[] {
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key2),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key3),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key4),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key5),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key6),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key7),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key8),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key9),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key0),
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

    private boolean tryInputGenericPassword(Activity activity, String password) {

        EditText pwdEditText = findPasswordEditText(activity);
        L.d("pwdEditText", pwdEditText);
        if (pwdEditText == null) {
            return false;
        }
        View confirmPwdBtn = findConfirmPasswordBtn(activity);
        L.d("confirmPwdBtn", confirmPwdBtn);
        if (confirmPwdBtn == null) {
            return false;
        }
        pwdEditText.setText(password);
        confirmPwdBtn.performClick();
        return true;
    }

    private EditText findPasswordEditText(Activity activity) {
        View pwdEditText = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password");
        if (pwdEditText instanceof EditText) {
            if (!pwdEditText.isShown()) {
                return null;
            }
            return (EditText) pwdEditText;
        }
        // long password
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, "", outList);
        for (View view : outList) {
            if (view instanceof EditText) {
                if (view.getId() != -1) {
                    continue;
                }
                if (!view.isShown()) {
                    continue;
                }
                return (EditText) view;
            }
        }
        return null;
    }

    private View findConfirmPasswordBtn(Activity activity) {
        View okView =  ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "button_ok");
        L.d("okView", okView);
        if (okView != null) {
            if (!okView.isShown()) {
                return null;
            }
            return okView;
        }
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, "付款", outList);
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(rootView, "Pay", outList);
        }
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(rootView, "确定", outList);
        }
        int versionCode = getVersionCode(activity);
        for (View view : outList) {
            if (view.getId() != -1) {
                continue;
            }
            if (!view.isShown()) {
                continue;
            }
            if (versionCode >= 1261 /** 10.5.96.8000 */) {
                return view;
            }
            return (View) view.getParent();
        }
        return null;
    }

    private void hidePreviousPayDialog() {
        AlertDialog dialog = mFingerPrintAlertDialog;
        L.d("hidePreviousPayDialog", mFingerPrintAlertDialog);
        if (dialog != null) {
            dialog.dismiss();
        }
        mFingerPrintAlertDialog = null;
    }
}
