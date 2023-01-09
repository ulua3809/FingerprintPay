package com.surcumference.fingerprint.plugin;

import static com.surcumference.fingerprint.Constant.ICON_ALIPAY_SETTING_ENTRY_BASE64;
import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_ALIPAY;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.AlipayVersionControl;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.ImageUtils;
import com.surcumference.fingerprint.util.NotifyUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.AlipayPayView;
import com.surcumference.fingerprint.view.DialogFrameLayout;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;

import java.util.ArrayList;
import java.util.List;

public class AlipayBasePlugin {


    private AlertDialog mFingerPrintAlertDialog;
    private boolean mPwdActivityDontShowFlag;
    private int mPwdActivityReShowDelayTimeMsec;

    private FingerprintIdentify mFingerprintIdentify;
    private Activity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mAlipayVersionCode;

    private int getAlipayVersionCode(Context context) {
        if (mAlipayVersionCode != 0) {
            return mAlipayVersionCode;
        }
        mAlipayVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_ALIPAY);
        return mAlipayVersionCode;
    }

    protected void onActivityCreated(Activity activity) {
        L.d("activity", activity);
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".MySettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject_10_1_38(activity));
            } else if (activityClzName.contains(".UserSettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject(activity));
            } else if (activityClzName.contains(".PayPwdDialogActivity")
                    || activityClzName.contains(".MspContainerActivity")
                    || activityClzName.contains(".FlyBirdWindowActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                mIsViewTreeObserverFirst = true;
                int alipayVersionCode = getAlipayVersionCode(activity);
                activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
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
                    if (alipayVersionCode >= 661 /** 10.3.10.8310 */) {
                        if (ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "simplePwdLayout") == null
                                && ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "mini_linSimplePwdComponent") == null
                                && ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password") == null ) {
                            return;
                        }

                        if (mIsViewTreeObserverFirst) {
                            if (showFingerPrintDialog(activity)) {
                                mIsViewTreeObserverFirst = false;
                            }
                        }
                        return;
                    }
                    if (ViewUtils.findViewByName(activity, (alipayVersionCode >= 352 /** 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout") == null
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
                activity.getWindow().getDecorView().setAlpha(0);
                Task.onMain(1500, () -> {
                    int versionCode = getAlipayVersionCode(activity);
                    AlipayVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
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


    public void onActivityResumed(Activity activity) {
        L.d("activity resumed", activity);
        mCurrentActivity = activity;
    }

    public void initFingerPrintLock(final Context context, final Runnable onSuccessUnlockCallback) {
        mFingerprintIdentify = new FingerprintIdentify(context);
        mFingerprintIdentify.setSupportAndroidL(true);
        mFingerprintIdentify.init();
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(5, new BaseFingerprint.IdentifyListener() {
                @Override
                public void onSucceed() {
                    L.d("指纹识别成功");
                    onSuccessUnlockCallback.run();
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    L.d("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_match));
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    L.d("多次尝试错误，请确认指纹 isDeviceLocked", isDeviceLocked);
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_retry_ended));
                    AlertDialog dialog = mFingerPrintAlertDialog;
                    if (dialog != null) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_unlock_reboot));
                }
            });
        } else {
            L.d("系统指纹功能未启用");
            NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_enable));
        }
    }

    public boolean showFingerPrintDialog(final Activity activity) {
        final Context context = activity;
        try {
            if (getAlipayVersionCode(activity) >= 224) {
                if (activity.getClass().getName().contains(".MspContainerActivity")) {
                    View payTextView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "支付宝支付密码", "支付寶支付密碼", "Alipay Payment Password");
                    L.d("payTextView", payTextView);
                    if (payTextView == null) {
                        return false;
                    }
                }
            }

            hidePreviousPayDialog();
            activity.getWindow().getDecorView().setAlpha(0);
            mPwdActivityDontShowFlag = false;
            mPwdActivityReShowDelayTimeMsec = 0;
            clickDigitPasswordWidget(activity);
            initFingerPrintLock(context, () -> {
                BlackListUtils.applyIfNeeded(context);
                String pwd = Config.from(activity).getPassword();
                if (TextUtils.isEmpty(pwd)) {
                    Toast.makeText(activity, Lang.getString(R.id.toast_password_not_set_alipay), Toast.LENGTH_SHORT).show();
                    return;
                }

                Runnable onCompleteRunnable = () -> {
                    mPwdActivityReShowDelayTimeMsec = 1000;
                    AlertDialog dialog = mFingerPrintAlertDialog;
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                };

                if (!tryInputGenericPassword(activity, pwd)) {
                    boolean tryAgain = false;
                    try {
                        inputDigitPassword(activity, pwd);
                    } catch (NullPointerException e) {
                        tryAgain = true;
                    } catch (Exception e) {
                        Toast.makeText(context, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                        L.e(e);
                    }
                    if (tryAgain) {
                        clickDigitPasswordWidget(activity);
                        Task.onMain(1000, ()-> {
                            try {
                                inputDigitPassword(activity, pwd);
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
                }
                onCompleteRunnable.run();
            });
            DialogFrameLayout alipayPayView = new AlipayPayView(context).withOnCloseImageClickListener(v -> {
                mPwdActivityDontShowFlag = true;
                AlertDialog dialog1 = mFingerPrintAlertDialog;
                if (dialog1 != null) {
                    dialog1.dismiss();
                }
                activity.onBackPressed();
            }).withOnDismissListener(v -> {
                FingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
                if (fingerprintIdentify != null) {
                    fingerprintIdentify.cancelIdentify();
                }
                if (!mPwdActivityDontShowFlag) {
                    Task.onMain(mPwdActivityReShowDelayTimeMsec, () -> activity.getWindow().getDecorView().setAlpha(1));
                }
            });
            Task.onMain(100,  () -> mFingerPrintAlertDialog = alipayPayView.showInDialog());
        } catch (OutOfMemoryError e) {
        }
        return true;
    }

    /**
     * 修复某个设备不自动弹出键盘
     * @param activity
     */
    private void clickDigitPasswordWidget(Activity activity) {
        int versionCode = getAlipayVersionCode(activity);
        View view = ViewUtils.findViewByName(activity, (versionCode >= 352 /** 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout");
        L.d("digit password widget", view);
        if (view == null) {
            return;
        }
        ViewUtils.performActionClick(view);
    }

    private void doSettingsMenuInject_10_1_38(final Activity activity) {
        int listViewId = activity.getResources().getIdentifier("setting_list", "id", "com.alipay.android.phone.openplatform");

        ListView listView = activity.findViewById(listViewId);

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

        //try use Alipay style
        try {
            View settingsView = ViewUtils.findViewByName(activity, "com.alipay.mobile.antui", "item_left_text");
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

        int versionCode = getAlipayVersionCode(activity);
        if (versionCode >= 661 /** 10.3.10.8310 */) {
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

        listView.addHeaderView(rootLinearLayout);
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
        int versionCode = getAlipayVersionCode(activity);
        AlipayVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
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
        L.d("pwdEditText1", pwdEditText);
        if (pwdEditText instanceof EditText) {
            if (!pwdEditText.isShown()) {
                return null;
            }
            return (EditText) pwdEditText;
        }
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
        for (View view : outList) {
            if (view.getId() != -1) {
                continue;
            }
            if (!view.isShown()) {
                continue;
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
