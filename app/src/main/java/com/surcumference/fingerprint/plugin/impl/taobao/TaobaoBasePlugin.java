package com.surcumference.fingerprint.plugin.impl.taobao;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_TAOBAO;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.plugin.inf.OnFingerprintVerificationOKListener;
import com.surcumference.fingerprint.util.ActivityViewObserver;
import com.surcumference.fingerprint.util.ActivityViewObserverHolder;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BizBiometricIdentify;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.TaobaoVersionControl;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.XBiometricIdentify;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.AlipayPayView;
import com.surcumference.fingerprint.view.DialogUtils;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import java.util.ArrayList;
import java.util.List;

public class TaobaoBasePlugin implements IAppPlugin {

    private AlertDialog mFingerPrintAlertDialog;
    private int mPwdActivityReShowDelayTimeMsec;

    private LinearLayout mItemHlinearLayout;
    private LinearLayout mLineTopCon;
    private View mLineBottomView;

    private XBiometricIdentify mFingerprintIdentify;

    private Activity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mTaobaoVersionCode = 0;

    @Override
    public int getVersionCode(Context context) {
        if (mTaobaoVersionCode != 0) {
            return mTaobaoVersionCode;
        }
        mTaobaoVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_TAOBAO);
        return mTaobaoVersionCode;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        L.d("activity", activity);
        handleSettingsMenuInjection(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        //Xposed not hooked yet!
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

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            mCurrentActivity = activity;
            int versionCode = getVersionCode(activity);
            if (activityClzName.contains(".PayPwdDialogActivity")
                    || activityClzName.contains(".MspContainerActivity")
                    || activityClzName.contains(".FlyBirdWindowActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                mIsViewTreeObserverFirst = true;
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
                    boolean isRechargePay = (ViewUtils.isShown(ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password"))
                            && ViewUtils.isShown(ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "keyboard_container")));

                    boolean isNormalPay = ViewUtils.isShown(ViewUtils.findViewByText(rootView,
                            "支付宝支付密码", "支付寶支付密碼", "Alipay Payment Password", //兼容上一版本的支付组件, 参考 10.28.10(473)
                            "请输入长密码", "請輸入長密碼", "Payment Password"))
                            || ViewUtils.isShown(ViewUtils.findViewByText(rootView,"密码共6位，已输入0位"));
                    if (isRechargePay || isNormalPay) {
                        if (mIsViewTreeObserverFirst) {
                            mIsViewTreeObserverFirst = false;
                            showFingerPrintDialog(activity);
                        }
                        return;
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
                    TaobaoVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = TaobaoVersionControl.getDigitPasswordKeyPad(mTaobaoVersionCode);
                    View key1View = ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1);
                    if (key1View != null) {
                        showFingerPrintDialog(activity);
                        return;
                    }

                    //try again
                    Task.onMain(2000, () -> showFingerPrintDialog(activity));
                });
            } else if (versionCode >= 323 /** 9.20.0 */ && handleSettingsMenuInjection(activity)) {
                return;
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    protected boolean handleSettingsMenuInjection(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".NewTaobaoSettingActivity") // <- 这个命名真是SB, 再改版一次估计你要叫 NewNewTaobaoSettingActivity 了吧
                    || activityClzName.contains(".TaobaoSettingActivity")) {
                Task.onMain(250, () -> doSettingsMenuInject(activity)); // 100 -> 250, 这个改版的页面加载性能还没上一版好
                Task.onMain(1000, () -> doSettingsMenuInject(activity)); // try again
                return true;
            }
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    public void initFingerPrintLock(final Context context,
                                    AlertDialog dialog, String passwordEncrypted,
                                    final OnFingerprintVerificationOKListener onSuccessUnlockCallback) {
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

    public void showFingerPrintDialog(final Activity activity) {
        final Context context = activity;
        final Config config = Config.from(context);
        try {
            String passwordEncrypted = config.getPasswordEncrypted();
            if (TextUtils.isEmpty(passwordEncrypted) || TextUtils.isEmpty(config.getPasswordIV())) {
                Toaster.showLong(Lang.getString(R.id.toast_password_not_set_taobao));
                return;
            }
            mPwdActivityReShowDelayTimeMsec = 0;
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
                                    Task.onMain(1000, ()-> {
                                        try {
                                            inputDigitPassword(activity, password);
                                        } catch (NullPointerException e) {
                                            Toaster.showLong(Lang.getString(R.id.toast_password_auto_enter_fail));
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
            }).withOnCancelButtonClickListener(target -> {
                DialogUtils.dismiss(target.getDialog());
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
    }

    private void setupPaymentItemOnClickListener(ViewGroup rootView) {
        List<View> paymentMethodsViewList = new ArrayList<>();
        ViewUtils.getChildViewsByRegex(rootView, ".*选中,.+", paymentMethodsViewList);
        long paymentMethodsViewListSize = paymentMethodsViewList.size();
        for (int i = 0; i < paymentMethodsViewListSize; i++) {
            if (i == paymentMethodsViewListSize - 1) {
                // 最后一个
                continue;
            }
            View paymentMethodView = paymentMethodsViewList.get(i);
            L.d("paymentMethodView", ViewUtils.getViewInfo(paymentMethodView));
            // 只取第一次, 防止多次调用造成出错
            View.OnClickListener originPaymentMethodListener = (View.OnClickListener) paymentMethodView.getTag(R.id.alipay_payment_method_item_click_listener);
            if (originPaymentMethodListener == null) {
                paymentMethodView.setTag(R.id.alipay_payment_method_item_click_listener, ViewUtils.getOnClickListener(paymentMethodView));
            }
            paymentMethodView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        AlertDialog dialog = mFingerPrintAlertDialog;
                        if (dialog == null) {
                            return;
                        }
                        if (!dialog.isShowing()) {
                            dialog.show();
                        }
                    } finally {
                        View.OnClickListener originPaymentMethodListener = (View.OnClickListener) v.getTag(R.id.alipay_payment_method_item_click_listener);
                        if (originPaymentMethodListener != null && originPaymentMethodListener != this) {
                            originPaymentMethodListener.onClick(v);
                        }
                    }
                }
            });
        }
    }

    private void reEnteredPayDialogSolution(Activity activity) {
        int versionCode = getVersionCode(activity);
        if (versionCode < 643 /** 10.36.10 */) {
            return;
        }
        ViewGroup rootView = (ViewGroup)activity.getWindow().getDecorView();
        setupPaymentItemOnClickListener(rootView);
        // 在10s内寻找密码框
        ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
        activityViewObserver.setActivityViewFinder(outViewList -> {
            EditText view = findPasswordEditText(activity);
            if (view != null) {
                outViewList.add(view);
            }
            View shortPwdView = ViewUtils.findViewByText(rootView, "密码共6位，已输入0位");
            if (ViewUtils.isShown(shortPwdView)) {
                outViewList.add(shortPwdView);
            }
        });
        ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.TaobaoPasswordView,
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
                                        // 如果支付方式点了第一项, 页面会刷新, 需要重建OnclickListener
                                        Task.onMain(666, () -> {
                                            setupPaymentItemOnClickListener(rootView);
                                        });
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
     * 感觉是淘宝出bug了, 控件的id 和Resources 获取到的id 不符
     */
    @Nullable
    private View findTaobaoVSettingsPageItemView(@NonNull ViewGroup rootView) {
        List<View> childViewList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, childViewList);
        String packageName = rootView.getContext().getPackageName();
        String identifier = packageName + ":id/v_setting_page_item} ";
        ViewUtils.sortViewListByYPosition(childViewList);
        for (View childView: childViewList) {
            if (ViewUtils.getViewInfo(childView).contains(identifier)) {
                if (childView.isShown()) {
                    return childView;
                }
            }
        }
        return null;
    }

    private void doSettingsMenuInject(final Activity activity) {
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        if (ViewUtils.findViewByText(rootView, Lang.getString(R.id.app_settings_name)) != null) {
            return;
        }
        View itemView = ViewUtils.findViewByName(activity, activity.getPackageName(), "v_setting_page_item");
        if (itemView == null) {
            itemView = findTaobaoVSettingsPageItemView(rootView);
        }
        LinearLayout linearLayout = (LinearLayout) itemView.getParent();
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

        if (mLineTopCon == null) {
            mLineTopCon = new LinearLayout(activity);
        } else {
            ViewUtils.removeFromSuperView(mLineTopCon);
        }
        mLineTopCon.setPadding(DpUtils.dip2px(activity, 12), 0, 0, 0);
        mLineTopCon.setBackgroundColor(Color.WHITE);

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFDFDFDF);
        mLineTopCon.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        if (mItemHlinearLayout == null) {
            mItemHlinearLayout = new LinearLayout(activity);
        } else {
            ViewUtils.removeFromSuperView(mItemHlinearLayout);
            mItemHlinearLayout.removeAllViews();
        }
        mItemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        mItemHlinearLayout.setWeightSum(1);
        mItemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).create());
        mItemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        mItemHlinearLayout.setClickable(true);
        mItemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());


        TextView itemNameText = new TextView(activity);

        int defHPadding = DpUtils.dip2px(activity, 13);
        itemNameText.setTextColor(0xFF051B28);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF999999);

        //try use Taobao style
        try {
            View generalView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "通用", "General");
            L.d("generalView", generalView);
            if (generalView instanceof TextView) {
                TextView generalTextView = (TextView) generalView;
                float scale = itemNameText.getTextSize() / generalTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, generalTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(generalTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        mItemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        mItemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (mLineBottomView == null) {
            mLineBottomView = new View(activity);
        } else {
            ViewUtils.removeFromSuperView(mLineBottomView);
        }
        mLineBottomView.setBackgroundColor(0xFFDFDFDF);

        linearLayout.addView(mLineTopCon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(mItemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 44)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtils.dip2px(activity, 10);
        linearLayout.addView(mLineBottomView, lineParams);

        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    private void inputDigitPassword(Activity activity, String password) {
        TaobaoVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = TaobaoVersionControl.getDigitPasswordKeyPad(mTaobaoVersionCode);
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
        View pwdEditText = ViewUtils.findViewByName(activity, "com.taobao.taobao", "input_et_password");;
        if (pwdEditText instanceof EditText) {
            if (!pwdEditText.isShown()) {
                return null;
            }
            return (EditText) pwdEditText;
        }
        // long password
        ViewGroup viewGroup = (ViewGroup)activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(viewGroup, "", outList);
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
        View okView =  ViewUtils.findViewByName(activity, "com.taobao.taobao", "button_ok");
        if (okView != null) {
            if (!okView.isShown()) {
                return null;
            }
            return okView;
        }
        ViewGroup rootView = (ViewGroup)activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViewsByRegex(rootView, "确定|確定|OK|确认|付款|確認|Pay", outList);
        int versionCode = getVersionCode(activity);
        for (View view : outList) {
            if (view.getId() != -1) {
                continue;
            }
            if (!view.isShown()) {
                continue;
            }
            // 跳过键盘上的OK
            if (view.getParent().toString().contains(":id/key_enter")) {
                continue;
            }
            if (versionCode < 643 /** 10.36.10 */) {
                return (View) view.getParent();
            }
            return view;
        }
        return null;
    }
}
