package com.surcumference.fingerprint.plugin.impl.qq;

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

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.plugin.inf.IMockCurrentUser;
import com.surcumference.fingerprint.plugin.inf.OnFingerprintVerificationOKListener;
import com.surcumference.fingerprint.util.AESUtils;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.KeyboardUtils;
import com.surcumference.fingerprint.util.QQUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.XFingerprintIdentify;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.util.paydialog.QQPayDialog;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.crypto.Cipher;

public class QQBasePlugin_8_2_11 implements IAppPlugin, IMockCurrentUser {

    private static final String TAG_FINGER_PRINT_IMAGE = "FINGER_PRINT_IMAGE";
    private static final String TAG_PASSWORD_EDITTEXT = "TAG_PASSWORD_EDITTEXT";
    private static final String TAG_LONGPASSWORD_OK_BUTTON = "TAG_LONGPASSWORD_OK_BUTTON";
    private static final String TAG_ACTIVITY_PAY = "TAG_ACTIVITY_PAY";
    private static final String TAG_ACTIVITY_FIRST_RESUME = "TAG_ACTIVITY_FIRST_RESUME";

    protected static final int QQ_VERSION_CODE_7_3_0 = 750;
    protected static final int QQ_VERSION_CODE_8_8_83 = 2654;

    private XFingerprintIdentify mFingerprintIdentify;
    private LinearLayout mMenuItemLLayout;

    protected boolean mMockCurrentUser = false;
    private Activity mCurrentPayActivity;
    private boolean mFingerprintScanStateReady = false;
    private WeakHashMap<Activity, String> mActivityPayMap = new WeakHashMap<>();
    private WeakHashMap<Activity, String> mActivityResumeMap = new WeakHashMap<>();
    private WeakHashMap<Activity, QQPayDialog> mActivityPayDialogMap = new WeakHashMap<>();
    private int mQQVersionCode;
    private ViewTreeObserver.OnWindowAttachListener mPayWindowAttachListener;

    public int getVersionCode(Context context) {
        if (mQQVersionCode != 0) {
            return mQQVersionCode;
        }
        mQQVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_QQ);
        return mQQVersionCode;
    }

    @Override
    public void onActivityCreated(Activity activity) {
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

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".SplashActivity")) {
                QQUtils.checkBlackListQQ(activity);
            }
            if (activityClzName.contains(".QWalletPluginProxyActivity")
                    || activityClzName.contains(".QWalletToolFragmentActivity")) {
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
                cancelFingerprintIdentify();
                initPayActivity(activity, 10, 100);
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
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

    @Override
    public boolean getMockCurrentUser() {
        return this.mMockCurrentUser;
    }

    @Override
    public void setMockCurrentUser(boolean mock) {
        this.mMockCurrentUser = mock;
    }

    private synchronized void initPayActivity(Activity activity, int retryDelay, int retryCountdown) {
        Context context = activity;
        Config config = Config.from(context);
        String passwordEncrypted = config.getPasswordEncrypted();
        if (TextUtils.isEmpty(passwordEncrypted) || TextUtils.isEmpty(config.getPasswordIV())) {
            Toaster.showLong(Lang.getString(R.id.toast_password_not_set_qq));
            return;
        }

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
        setupPayWindowAttachListener(payDialog.inputEditText);
        View fingerprintView = prepareFingerprintView(context);
        int versionCode = getVersionCode(context);

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
        initFingerPrintLock(context, (cipher) -> { // success
            String password = AESUtils.decrypt(cipher, passwordEncrypted);
            if (TextUtils.isEmpty(password)) {
                Toaster.showShort(Lang.getString(R.id.toast_fingerprint_password_dec_failed));
                return;
            }
            payDialog.inputEditText.setText(password);
            if (longPassword) {
                payDialog.okButton.performClick();
            }
        }, switchToPwdRunnable /** fail */);

        markAsPayActivity(activity);
        switchToFingerprintRunnable.run();
        for (int i = 10; i < 500; i += 20) {
            Task.onMain(i, switchToFingerprintRunnable);
        }
    }

    private void setupPayWindowAttachListener(View targetView) {
        ViewTreeObserver viewTreeObserver = targetView.getViewTreeObserver();
        if (mPayWindowAttachListener != null) {
            viewTreeObserver.removeOnWindowAttachListener(mPayWindowAttachListener);
            mPayWindowAttachListener = null;
        }
        mPayWindowAttachListener = new ViewTreeObserver.OnWindowAttachListener() {
            @Override
            public void onWindowAttached() {

            }

            @Override
            public void onWindowDetached() {
                viewTreeObserver.removeOnWindowAttachListener(this);
                mPayWindowAttachListener = null;
                cancelFingerprintIdentify();
            }
        };
        viewTreeObserver.addOnWindowAttachListener(mPayWindowAttachListener);
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
        if (getVersionCode(context) >= QQ_VERSION_CODE_7_3_0) {
            //QQ居然拿键盘底部来定位... To TP工程师. 你们的Activity太重了
            params.bottomMargin = DpUtils.dip2px(context, 30);
        }
        textView.setLayoutParams(params);
        return textView;
    }

    public void initFingerPrintLock(final Context context, OnFingerprintVerificationOKListener onSuccessUnlockCallback, final Runnable onFailureUnlockCallback) {
        L.d("initFingerPrintLock");
        cancelFingerprintIdentify();
        mFingerprintIdentify = new XFingerprintIdentify(context)
                .withMockCurrentUserCallback(this)
                .startIdentify(new XFingerprintIdentify.IdentifyListener() {
                    @Override
                    public void onSucceed(XFingerprintIdentify target, Cipher cipher) {
                        super.onSucceed(target, cipher);
                        onSuccessUnlockCallback.onFingerprintVerificationOK(cipher);
                    }

                    @Override
                    public void onFailed(XFingerprintIdentify target, FingerprintIdentifyFailInfo failInfo) {
                        super.onFailed(target, failInfo);
                        onFailureUnlockCallback.run();
                    }
                });
    }

    private void cancelFingerprintIdentify() {
        L.d("cancelFingerprintIdentify");
        XFingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
        if (fingerprintIdentify == null) {
            return;
        }
        if (!fingerprintIdentify.fingerprintScanStateReady) {
            return;
        }
        fingerprintIdentify.cancelIdentify();
    }

    private void resumeFingerprintIdentify() {
        L.d("resumeFingerprintIdentify");
        XFingerprintIdentify fingerprintIdentify = mFingerprintIdentify;
        if (fingerprintIdentify == null) {
            return;
        }
        if (fingerprintIdentify.fingerprintScanStateReady) {
            return;
        }
        fingerprintIdentify.resumeIdentify();
    }

    private void doSettingsMenuInject(final Activity activity) {
        boolean isDarkMode = StyleUtils.isDarkMode(activity);
        Context context = activity;
        int versionCode = getVersionCode(context);
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        View itemView = ViewUtils.findViewByText(rootView, "帐号管理");
        View aboutView = versionCode >= QQ_VERSION_CODE_8_8_83 ?
                ViewUtils.findViewByText(rootView, "关于QQ与帮助") : itemView;
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
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(isDarkMode? Color.BLACK : Color.WHITE).create());
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
            View settingsView = aboutView;
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
            View settingsItemView = (View)settingsView.getParent();
            if (settingsItemView != null) {
                Drawable background = settingsItemView.getBackground();
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
        int menuItemHeight = DpUtils.dip2px(activity, versionCode >= QQ_VERSION_CODE_8_8_83 ? 56 : 45);
        menuItemLLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, menuItemHeight));
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
