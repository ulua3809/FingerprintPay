package com.surcumference.fingerprint.plugin;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_TAOBAO;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.NotifyUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.TaobaoVersionControl;
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

public class TaobaoBasePlugin {

    private AlertDialog mFingerPrintAlertDialog;
    private boolean mPwdActivityDontShowFlag;
    private int mPwdActivityReShowDelayTimeMsec;

    private LinearLayout mItemHlinearLayout;
    private LinearLayout mLineTopCon;
    private View mLineBottomView;

    private FingerprintIdentify mFingerprintIdentify;

    private Activity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mTaobaoVersionCode = 0;

    private int getTaobaoVersionCode(Context context) {
        if (mTaobaoVersionCode != 0) {
            return mTaobaoVersionCode;
        }
        mTaobaoVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_TAOBAO);
        return mTaobaoVersionCode;
    }

    public void onActivityCreated(Activity activity) {
        L.d("activity", activity);
        handleSettingsMenuInjection(activity);
    }

    public void onActivityResumed(Activity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            mCurrentActivity = activity;
            int versionCode = getTaobaoVersionCode(activity);
            if (activityClzName.contains(versionCode >= 301 ? ".PayPwdDialogActivity" : ".MspContainerActivity")
                    || activityClzName.contains(".FlyBirdWindowActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                mIsViewTreeObserverFirst = true;
                activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (mCurrentActivity == null) {
                        return;
                    }
                    if (activity.isDestroyed()) {
                        return;
                    }
                    if (mCurrentActivity != activity) {
                        return;
                    }
                    if (ViewUtils.findViewByName(activity, "com.taobao.taobao", "mini_spwd_input") == null
                            && ViewUtils.findViewByName(activity, "com.taobao.taobao", "simplePwdLayout") == null
                            && ViewUtils.findViewByName(activity, "com.taobao.taobao", "input_et_password") == null ) {
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

    public void initFingerPrintLock(final Context context, final Runnable onSuccessUnlockCallback) {
        mFingerprintIdentify = new FingerprintIdentify(context);
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_match));
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
                    L.d("多次尝试错误，请使用密码输入");
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

    public void showFingerPrintDialog(final Activity activity) {
        final Context context = activity;
        try {
            activity.getWindow().getDecorView().setAlpha(0);
            mPwdActivityDontShowFlag = false;
            mPwdActivityReShowDelayTimeMsec = 0;
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
                        Task.onMain(1000, ()-> {
                            try {
                                inputDigitPassword(activity, pwd);
                            } catch (NullPointerException e) {
                                Toast.makeText(context, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
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
                AlertDialog dialog = mFingerPrintAlertDialog;
                if (dialog != null) {
                    dialog.dismiss();
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
        View pwdEditText = ViewUtils.findViewByName(activity, "com.taobao.taobao", "input_et_password");;
        if (pwdEditText instanceof EditText) {
            if (!pwdEditText.isShown()) {
                return null;
            }
            return (EditText) pwdEditText;
        }
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
        ViewGroup viewGroup = (ViewGroup)activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(viewGroup, "确认", outList);
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(viewGroup, "付款", outList);
        }
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(viewGroup, "確認", outList);
        }
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(viewGroup, "Pay", outList);
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
}
