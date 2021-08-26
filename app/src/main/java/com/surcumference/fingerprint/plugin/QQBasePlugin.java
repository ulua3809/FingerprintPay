package com.surcumference.fingerprint.plugin;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_QQ;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.KeyboardUtils;
import com.surcumference.fingerprint.util.PermissionUtils;
import com.surcumference.fingerprint.util.QQUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.util.paydialog.QQPayDialog;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class QQBasePlugin {

    private static final String TAG_FINGER_PRINT_IMAGE = "FINGER_PRINT_IMAGE";
    private static final String TAG_PASSWORD_EDITTEXT = "TAG_PASSWORD_EDITTEXT";
    private static final String TAG_LONGPASSWORD_OK_BUTTON = "TAG_LONGPASSWORD_OK_BUTTON";
    private static final String TAG_ACTIVITY_PAY = "TAG_ACTIVITY_PAY";
    private static final String TAG_ACTIVITY_FIRST_RESUME = "TAG_ACTIVITY_FIRST_RESUME";

    private static final int QQ_VERSION_CODE_7_3_0 = 750;

    private FingerprintIdentify mFingerprintIdentify;
    private LinearLayout mMenuItemLLayout;

    protected boolean mMockCurrentUser = false;
    private Activity mCurrentPayActivity;
    private boolean mFingerprintScanStateReady = false;
    private WeakHashMap<Activity, String> mActivityPayMap = new WeakHashMap<>();
    private WeakHashMap<Activity, String> mActivityResumeMap = new WeakHashMap<>();
    private WeakHashMap<Activity, QQPayDialog> mActivityPayDialogMap = new WeakHashMap<>();
    private int mQQVersionCode;

    private int getQQVersionCode(Context context) {
        if (mQQVersionCode != 0) {
            return mQQVersionCode;
        }
        mQQVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_QQ);
        return mQQVersionCode;
    }

    protected void onActivityCreated(Activity activity) {
        L.d("activity", activity);
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".QQSettingSettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject(activity));
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    protected void onActivityResumed(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".SplashActivity")) {
                QQUtils.checkBlackListQQ(activity);
            }
            if (activityClzName.contains(".QWalletPluginProxyActivity")) {
                L.d("found");
                if (!Config.from(activity).isOn()) {
                    return;
                }
                if (isActivityFirstResume(activity)) {
                    markActivityResumed(activity);
                    qqKeyboardFlashBugfixer(activity);
                    qqKeyboardLazyBugfixer(activity);
                    qqTitleBugfixer(activity);
                } else if (isPayActivity(activity)) {
                    qqKeyboardFlashBugfixer(activity);
                }
                initPayActivity(activity, 10, 100);
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    protected void onActivityPaused(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".QWalletPluginProxyActivity")) {
                if (activity == mCurrentPayActivity) {
                    L.d("found");
                    mCurrentPayActivity = null;
                    cancelFingerprintIdentify();
                }
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    private void initPayActivity(Activity activity, int retryDelay, int retryCountdown) {
        Context context = activity;
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();

        QQPayDialog _payDialog = mActivityPayDialogMap.get(activity);
        if (_payDialog == null) {
            _payDialog = QQPayDialog.findFrom(rootView);
            mActivityPayDialogMap.put(activity, _payDialog);
        }
        if (_payDialog == null) {
            if (retryCountdown > 0) {
                Task.onMain(retryDelay, () -> {
                    initPayActivity(activity, retryDelay, retryCountdown - 1);
                });
            }
            return;
        }
        QQPayDialog payDialog = _payDialog;
        boolean longPassword = payDialog.isLongPassword();
        ViewGroup editCon = longPassword ? (ViewGroup) payDialog.inputEditText.getParent().getParent().getParent()
                : (ViewGroup) payDialog.inputEditText.getParent().getParent();
        View fingerprintView = prepareFingerprintView(context);
        int versionCode = getQQVersionCode(context);

        Runnable switchToPwdRunnable = () -> {
            if (activity != mCurrentPayActivity) {
                return;
            }
            if (editCon.getVisibility() != View.VISIBLE) {
                editCon.setVisibility(View.VISIBLE);
            }
            if (longPassword) {
                KeyboardUtils.switchIme(payDialog.inputEditText, true);
                payDialog.inputEditText.requestFocus();
            } else {
                payDialog.keyboardView.setAlpha(1);
                if (payDialog.keyboardView.getVisibility() != View.VISIBLE) {
                    payDialog.keyboardView.setVisibility(View.VISIBLE);
                }
            }
            if (fingerprintView.getVisibility() != View.GONE) {
                fingerprintView.setVisibility(View.GONE);
            }
            if (payDialog.titleTextView != null) {
                if (versionCode >= QQ_VERSION_CODE_7_3_0) {
                    payDialog.titleTextView.setClickable(true);
                    payDialog.titleTextView.setText("找回密码");
                } else {
                    payDialog.titleTextView.setText(Lang.getString(R.id.qq_payview_password_title));
                }
            }
            if (payDialog.usePasswordText != null) {
                payDialog.usePasswordText.setText(Lang.getString(R.id.qq_payview_fingerprint_switch_text));
            }
            if (longPassword && payDialog.okButton != null) {
                if (payDialog.okButton.getVisibility() != View.VISIBLE) {
                    payDialog.okButton.setVisibility(View.VISIBLE);
                }
            }
            if (payDialog.withdrawTitleTextView != null) {
                payDialog.withdrawTitleTextView.setText("输入支付密码，验证身份");
            }
            cancelFingerprintIdentify();
        };

        Runnable switchToFingerprintRunnable = () -> {
            if (activity != mCurrentPayActivity) {
                return;
            }
            if (editCon.getVisibility() != View.GONE) {
                editCon.setVisibility(View.GONE);
            }
            if (longPassword) {
                KeyboardUtils.switchIme(payDialog.inputEditText, false);
                payDialog.inputEditText.clearFocus();
            } else {
                payDialog.keyboardView.setAlpha(0);
                if (payDialog.keyboardView.getVisibility() != View.INVISIBLE) {
                    payDialog.keyboardView.setVisibility(View.INVISIBLE);
                    //fix切换支付方式后键盘会出现
                    Task.onMain(1000, () -> {
                        if (mFingerprintScanStateReady) {
                            payDialog.keyboardView.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
            if (fingerprintView.getVisibility() != View.VISIBLE) {
                fingerprintView.setVisibility(View.VISIBLE);
            }
            if (payDialog.titleTextView != null) {
                payDialog.titleTextView.setText(Lang.getString(R.id.qq_payview_fingerprint_title));
                if (versionCode >= QQ_VERSION_CODE_7_3_0) {
                    payDialog.titleTextView.setClickable(false);
                }
            }
            if (payDialog.usePasswordText != null) {
                payDialog.usePasswordText.setText(Lang.getString(R.id.qq_payview_password_switch_text));
            }
            if (longPassword && payDialog.okButton != null) {
                if (payDialog.okButton.getVisibility() != View.GONE) {
                    payDialog.okButton.setVisibility(View.GONE);
                }
            }
            if (payDialog.withdrawTitleTextView != null) {
                payDialog.withdrawTitleTextView.setText("使用指纹验证身份");
            }
            resumeFingerprintIdentify();
        };

        fingerprintView.setOnClickListener(v -> {
            switchToPwdRunnable.run();
        });

        if (payDialog.usePasswordText != null) {
            payDialog.usePasswordText.setOnClickListener(v -> {
                if (Lang.getString(R.id.qq_payview_password_switch_text).equals(payDialog.usePasswordText.getText())) {
                    switchToPwdRunnable.run();
                } else {
                    switchToFingerprintRunnable.run();
                }
            });
            payDialog.usePasswordText.setVisibility(View.VISIBLE);
        }

        ViewGroup viewGroup = ((ViewGroup)(editCon.getParent()));
        removeAllFingerprintView(viewGroup);

        int keyboardViewPosition = ViewUtils.findChildViewPosition(viewGroup, payDialog.keyboardView);
        if (keyboardViewPosition >= 0) {
            viewGroup.addView(fingerprintView, keyboardViewPosition);
        } else {
            viewGroup.addView(fingerprintView);
        }

        mCurrentPayActivity = activity;
        initFingerPrintLock(context, () -> { // success
            Config config = Config.from(context);
            String pwd = config.getPassword();
            if (TextUtils.isEmpty(pwd)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_qq), Toast.LENGTH_SHORT).show();
                return;
            }
            payDialog.inputEditText.setText(pwd);
            if (longPassword) {
                payDialog.okButton.performClick();
            }
        }, () -> { //fail
            switchToPwdRunnable.run();
        });

        markAsPayActivity(activity);
        switchToFingerprintRunnable.run();
        for (int i = 10; i < 500; i += 20) {
            Task.onMain(i, switchToFingerprintRunnable);
        }
    }

    private void removeAllFingerprintView(ViewGroup viewGroup) {
        List<View> pendingRemoveList = new ArrayList<>();

        int childCount = viewGroup.getChildCount();
        for (int i = 0 ;i < childCount ; i++) {
            View view = viewGroup.getChildAt(i);
            if (TAG_FINGER_PRINT_IMAGE.equals(view.getTag())) {
                pendingRemoveList.add(view);
            }
        }

        for (View view : pendingRemoveList) {
            ViewUtils.removeFromSuperView(view);
        }
    }

    private View prepareFingerprintView(Context context) {

        TextView textView = new TextView(context);
        textView.setTag(TAG_FINGER_PRINT_IMAGE);
        textView.setText("使用密码");
        textView.setTextSize(16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        if (getQQVersionCode(context) >= QQ_VERSION_CODE_7_3_0) {
            //QQ居然拿键盘底部来定位... To TP工程师. 你们的Activity太重了
            params.bottomMargin = DpUtils.dip2px(context, 30);
        }
        textView.setLayoutParams(params);
        return textView;
    }

    public void initFingerPrintLock(final Context context, final Runnable onSuccessUnlockCallback, final Runnable onFailureUnlockCallback) {
        L.d("initFingerPrintLock");
        cancelFingerprintIdentify();
        mMockCurrentUser = true;
        FingerprintIdentify fingerprintIdentify = new FingerprintIdentify(context);
        if (fingerprintIdentify.isFingerprintEnable()) {
            mFingerprintScanStateReady = true;
            fingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_match), Toast.LENGTH_SHORT).show();
                    L.d("指纹识别成功");
                    onSuccessUnlockCallback.run();
                    mMockCurrentUser = false;
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    L.d("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                    Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_not_match), Toast.LENGTH_SHORT).show();
                    mMockCurrentUser = false;
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    if (mFingerprintScanStateReady) {
                        Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_retry_ended), Toast.LENGTH_SHORT).show();
                    }
                    L.d("多次尝试错误，请使用密码输入");
                    onFailureUnlockCallback.run();
                    mMockCurrentUser = false;
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_unlock_reboot), Toast.LENGTH_SHORT).show();
                    onFailureUnlockCallback.run();
                    mMockCurrentUser = false;
                }
            });
        } else {
            if (PermissionUtils.hasFingerprintPermission(context)) {
                L.d("系统指纹功能未启用");
                Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_not_enable), Toast.LENGTH_LONG).show();
            } else {
                L.d("QQ 版本过低");
                Toast.makeText(context, Lang.getString(R.id.toast_need_qq_7_2_5), Toast.LENGTH_LONG).show();
            }
            mMockCurrentUser = false;
            mFingerprintScanStateReady = false;
        }
        mFingerprintIdentify = fingerprintIdentify;
    }

    private void cancelFingerprintIdentify() {
        if (!mFingerprintScanStateReady) {
            return;
        }
        L.d("cancelFingerprintIdentify");
        mFingerprintScanStateReady = false;
        FingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
        if (fingerprintIdentify != null) {
            fingerprintIdentify.cancelIdentify();
        }
        mMockCurrentUser = false;
    }

    private void resumeFingerprintIdentify() {
        if (mFingerprintScanStateReady) {
            return;
        }
        L.d("resumeFingerprintIdentify");
        FingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
        if (fingerprintIdentify != null) {
            mMockCurrentUser = true;
            fingerprintIdentify.resumeIdentify();
            mFingerprintScanStateReady = true;
        }
    }

    private void doSettingsMenuInject(final Activity activity) {
        boolean isDarkMode = StyleUtils.isDarkMode(activity);
        Context context = activity;
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        View itemView = ViewUtils.findViewByText(rootView, "帐号管理");
        LinearLayout linearLayout = (LinearLayout) itemView.getParent().getParent().getParent();
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
        lineTopView.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(ViewUtils.genBackgroundDefaultDrawable(isDarkMode? Color.BLACK : Color.WHITE));
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtils.dip2px(activity, 16);

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF888888);

        //try use QQ style
        try {
            View settingsView = itemView;
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
            View generalItemView = (View)settingsView.getParent();
            if (generalItemView != null) {
                Drawable background = generalItemView.getBackground();
                if (background != null) {
                    Drawable.ConstantState constantState = background.getConstantState();
                    if (constantState != null) {
                        itemHlinearLayout.setBackground(constantState.newDrawable());
                    }
                }
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout menuItemLLayout = mMenuItemLLayout;
        if (menuItemLLayout == null) {
            menuItemLLayout = new LinearLayout(context);
            mMenuItemLLayout = menuItemLLayout;
        } else {
            ViewUtils.removeFromSuperView(menuItemLLayout);
            menuItemLLayout.removeAllViews();
        }

        menuItemLLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.topMargin = DpUtils.dip2px(activity, 20);
        menuItemLLayout.addView(lineTopView, lineParams);
        menuItemLLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 45)));
        lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        menuItemLLayout.addView(lineBottomView, lineParams);

        linearLayout.addView(menuItemLLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            if (view == menuItemLLayout) {
                continue;
            }
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    /**
     * 支付界面标题异常修复
     * @param activity
     */
    private void qqTitleBugfixer(Activity activity) {
        View titleView = ViewUtils.findViewByName(activity, "android", "title");
        ViewGroup contentView = (ViewGroup) ViewUtils.findViewByName(activity, "android", "content");
        if (titleView != null && contentView != null){

            activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (contentView.getChildCount() > 0) {
                    View firstChild = contentView.getChildAt(0);
                    Drawable backgroundDrawable;
                    if (firstChild != null && (backgroundDrawable = firstChild.getBackground()) instanceof ColorDrawable) {
                        titleView.setBackgroundColor(((ColorDrawable) backgroundDrawable).getColor());
                    } else {
                        titleView.setBackgroundColor(0x66000000);
                    }
                }
                if (titleView instanceof TextView) {
                    ((TextView) titleView).setText("");
                }
            });
        }
    }

    /**
     * 支付界面键盘闪现修复
     * @param activity
     */
    private void qqKeyboardFlashBugfixer(Activity activity) {
        View rootView = activity.getWindow().getDecorView();
        rootView.setAlpha(0);
        Task.onMain(200, () -> rootView.animate().alpha(1).start());

    }

    /**
     * 支付界面 指纹区域闪现修复
     * @param activity
     */
    private void qqKeyboardLazyBugfixer(Activity activity) {
        activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                List<View> keyboardViewList = new ArrayList<>();
                ViewUtils.getChildViewsByType((ViewGroup)activity.getWindow().getDecorView(), ".MyKeyboardWindow", keyboardViewList);
                if (keyboardViewList.size() == 0) {
                    return;
                }
                for (View view : keyboardViewList) {
                    View keyboardCon = (View) view.getParent();
                    if (keyboardCon == null) {
                        continue;
                    }
                    keyboardCon.setAlpha(0);
                }
                activity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Task.onMain(600, () -> {
                    for (View view : keyboardViewList) {

                        View keyboardCon = (View) view.getParent();
                        if (keyboardCon == null) {
                            continue;
                        }
                        keyboardCon.animate().alpha(1).start();
                    }
                });
            }
        });

    }

    private void markAsPayActivity(Activity activity) {
        mActivityPayMap.put(activity, TAG_ACTIVITY_PAY);
    }

    private boolean isPayActivity(Activity activity) {
        return TAG_ACTIVITY_PAY.equals(mActivityPayMap.get(activity));
    }

    private void markActivityResumed(Activity activity) {
        mActivityResumeMap.put(activity, TAG_ACTIVITY_FIRST_RESUME);
    }

    private boolean isActivityFirstResume(Activity activity) {
        return !TAG_ACTIVITY_FIRST_RESUME.equals(mActivityResumeMap.get(activity));
    }
}
