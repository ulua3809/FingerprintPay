package com.surcumference.fingerprint.plugin;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_WECHAT;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.ActivityViewObserver;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.BlackListUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.FragmentObserver;
import com.surcumference.fingerprint.util.ImageUtils;
import com.surcumference.fingerprint.util.NotifyUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.util.paydialog.WeChatPayDialog;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.WeakHashMap;

public class WeChatBasePlugin {

    private ActivityViewObserver mActivityViewObserver;
    private WeakHashMap<View, View.OnAttachStateChangeListener> mView2OnAttachStateChangeListenerMap = new WeakHashMap<>();
    protected boolean mMockCurrentUser = false;
    protected FingerprintIdentify mFingerprintIdentify;
    private FragmentObserver mFragmentObserver;

    private int mWeChatVersionCode = 0;

    private int getWeChatVersionCode(Context context) {
        if (mWeChatVersionCode != 0) {
            return mWeChatVersionCode;
        }
        mWeChatVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_WECHAT);
        return mWeChatVersionCode;
    }

    protected synchronized void initFingerPrintLock(Context context, Runnable onSuccessUnlockRunnable) {
        mMockCurrentUser = true;
        mFingerprintIdentify = new FingerprintIdentify(context.getApplicationContext());
        mFingerprintIdentify.setSupportAndroidL(true);
        mFingerprintIdentify.setExceptionListener(exception -> {
            if (exception instanceof SsdkUnsupportedException) {
                return;
            }
            L.e("fingerprint", exception);
        });
        mFingerprintIdentify.init();
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(5, new BaseFingerprint.IdentifyListener() {
                @Override
                public void onSucceed() {
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

    protected boolean isHeaderViewExistsFallback(ListView listView) {
        if (listView == null) {
            return false;
        }
        if (listView.getHeaderViewsCount() <= 0) {
            return false;
        }
        try {
            Field mHeaderViewInfosField = ListView.class.getDeclaredField("mHeaderViewInfos");
            mHeaderViewInfosField.setAccessible(true);
            ArrayList<ListView.FixedViewInfo> mHeaderViewInfos = (ArrayList<ListView.FixedViewInfo>) mHeaderViewInfosField.get(listView);
            if (mHeaderViewInfos != null) {
                for (ListView.FixedViewInfo viewInfo : mHeaderViewInfos) {
                    if (viewInfo.view == null) {
                        continue;
                    }
                    Object tag = viewInfo.view.getTag();
                    if (BuildConfig.APPLICATION_ID.equals(tag)) {
                        L.d("found plugin settings headerView");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    protected void onActivityResumed(Activity activity) {
        L.d("Activity onResume =", activity);
        final String activityClzName = activity.getClass().getName();
        if (activityClzName.contains("com.tencent.mm.plugin.setting.ui.setting.SettingsUI")
                || activityClzName.contains("com.tencent.mm.plugin.wallet.pwd.ui.WalletPasswordSettingUI")
                || activityClzName.contains("com.tencent.mm.ui.vas.VASCommonActivity") /** 8.0.18 */) {
            Task.onMain(100, () -> doSettingsMenuInject(activity));
        } else if (getWeChatVersionCode(activity) >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_20 && activityClzName.contains("com.tencent.mm.ui.LauncherUI")) {
            startFragmentObserver(activity);
        } else if (activityClzName.contains(".WalletPayUI")
                || activityClzName.contains(".UIPageFragmentActivity")) {
            stopAndRemoveCurrentActivityViewObserver();
            ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
            activityViewObserver.setViewIdentifyType(".EditHintPasswdView");
            activityViewObserver.start(100, new ActivityViewObserver.IActivityViewListener() {
                @Override
                public void onViewFounded(ActivityViewObserver observer, View view) {
                    ActivityViewObserver.IActivityViewListener l = this;
                    observer.stop();
                    L.d("onViewFounded:", view, " rootView: ", view.getRootView());

                    onPayDialogShown((ViewGroup) view.getRootView());
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
    }

    protected void onActivityPaused(Activity activity) {
        try {
            L.d("Activity onPause =", activity);
            final String activityClzName = activity.getClass().getName();
            if (activityClzName.contains(".WalletPayUI")
                || activityClzName.contains(".UIPageFragmentActivity")) {
                stopAndRemoveCurrentActivityViewObserver();
                onPayDialogDismiss(activity);
            } else if (getWeChatVersionCode(activity) >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_20 && activityClzName.contains("com.tencent.mm.ui.LauncherUI")) {
                stopFragmentObserver(activity);
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    private void startFragmentObserver(Activity activity) {
        stopFragmentObserver(activity);
        FragmentObserver fragmentObserver = new FragmentObserver(activity);
        fragmentObserver.setFragmentIdentifyClassName("com.tencent.mm.ui.vas.VASCommonFragment");
        fragmentObserver.start((observer, fragmentObject, fragmentRootView) -> doSettingsMenuInject(fragmentRootView.getContext(), fragmentRootView, fragmentObject.getClass().getName()));
        mFragmentObserver = fragmentObserver;
    }

    private void stopFragmentObserver(Activity activity) {
        FragmentObserver fragmentObserver = mFragmentObserver;
        if (fragmentObserver != null) {
            fragmentObserver.stop();
            mFragmentObserver = null;
        }
    }

    protected void onPayDialogShown(ViewGroup rootView) {
        L.d("PayDialog show");
        Context context = rootView.getContext();
        if (Config.from(context).isOn()) {
            int versionCode = getWeChatVersionCode(context);
            WeChatPayDialog payDialogView = WeChatPayDialog.findFrom(versionCode, rootView);
            L.d(payDialogView);
            if (payDialogView == null) {
                NotifyUtils.notifyVersionUnSupport(context, Constant.PACKAGE_NAME_WECHAT);
                return;
            }

            ViewGroup passwordLayout = payDialogView.passwordLayout;
            EditText mInputEditText = payDialogView.inputEditText;
            View keyboardView = payDialogView.keyboardView;
            TextView usePasswordText = payDialogView.usePasswordText;
            TextView titleTextView = payDialogView.titleTextView;

            RelativeLayout fingerPrintLayout = new RelativeLayout(context);
            fingerPrintLayout.setTag("fingerPrintLayout");
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            fingerPrintLayout.setLayoutParams(layoutParams);
            ImageView fingerprintImageView = new ImageView(context);

            try {
                final Bitmap bitmap = ImageUtils.base64ToBitmap(Constant.ICON_FINGER_PRINT_WECHAT_BASE64);
                fingerprintImageView.setImageBitmap(bitmap);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    fingerprintImageView.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                        @Override
                        public void onWindowAttached() {

                        }

                        @Override
                        public void onWindowDetached() {
                            fingerprintImageView.getViewTreeObserver().removeOnWindowAttachListener(this);
                            try {
                                bitmap.recycle();
                            } catch (Exception e) {
                            }
                        }
                    });
                }
            } catch (OutOfMemoryError e) {
                L.d(e);
            }
            fingerPrintLayout.addView(fingerprintImageView);


            final Runnable switchToFingerprintRunnable = ()-> {
                mInputEditText.setVisibility(View.GONE);
                keyboardView.setVisibility(View.GONE);
                View fingerPrintLayoutLast = passwordLayout.findViewWithTag("fingerPrintLayout");
                if (fingerPrintLayoutLast != null) {
                    passwordLayout.removeView(fingerPrintLayoutLast);
                }
                passwordLayout.addView(fingerPrintLayout);

                initFingerPrintLock(context, ()-> {
                    BlackListUtils.applyIfNeeded(context);
                    //SUCCESS UNLOCK
                    Config config = Config.from(context);
                    String pwd = config.getPassword();
                    if (TextUtils.isEmpty(pwd)) {
                        Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_wechat), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (getWeChatVersionCode(context) >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_18) {
                        mInputEditText.getText().clear();
                        for (char c : pwd.toCharArray()) {
                            mInputEditText.append(String.valueOf(c));
                        }
                    } else {
                        mInputEditText.setText(pwd);
                    }
                });
                if (titleTextView != null) {
                    titleTextView.setText(Lang.getString(R.id.wechat_payview_fingerprint_title));
                }
                if (usePasswordText != null) {
                    usePasswordText.setText(Lang.getString(R.id.wechat_payview_password_switch_text));
                }
            };

            final Runnable switchToPasswordRunnable = ()-> {
                passwordLayout.removeView(fingerPrintLayout);
                mInputEditText.setVisibility(View.VISIBLE);
                keyboardView.setVisibility(View.VISIBLE);
                mInputEditText.performClick();
                mFingerprintIdentify.cancelIdentify();
                mMockCurrentUser = false;
                if (titleTextView != null) {
                    titleTextView.setText(Lang.getString(R.id.wechat_payview_password_title));
                }
                if (usePasswordText != null) {
                    usePasswordText.setText(Lang.getString(R.id.wechat_payview_fingerprint_switch_text));
                }
            };

            if (usePasswordText != null) {
                Task.onMain(()-> usePasswordText.setVisibility(View.VISIBLE));
                usePasswordText.setOnTouchListener((view, motionEvent) -> {
                    try {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            if (mInputEditText.getVisibility() == View.GONE) {
                                switchToPasswordRunnable.run();
                            } else {
                                switchToFingerprintRunnable.run();
                            }
                        }
                    } catch (Exception e) {
                        L.e(e);
                    }
                    return true;
                });
            }

            fingerprintImageView.setOnClickListener(view -> switchToPasswordRunnable.run());
            switchToFingerprintRunnable.run();
        }
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
        doSettingsMenuInject(activity, activity.getWindow().getDecorView(), activity.getClass().getName());
    }

    protected void doSettingsMenuInject(Context context, View targetView, String targetClassName) {
        int versionCode = getWeChatVersionCode(context);
        ListView itemView = (ListView) ViewUtils.findViewByName(targetView, "android", "list");
        if (ViewUtils.findViewByText(itemView, Lang.getString(R.id.app_settings_name)) != null
                || isHeaderViewExistsFallback(itemView)) {
            return;
        }
        if (versionCode >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_18) {
            //整个设置界面的class 都是 com.tencent.mm.ui.vas.VASCommonActivity...
            if (targetClassName.contains("com.tencent.mm.ui.vas.VASCommonActivity")
                || targetClassName.contains("com.tencent.mm.ui.vas.VASCommonFragment") /** 8.0.20 */) {
                if (ViewUtils.findViewByText(itemView, Lang.getString(R.id.wechat_general),
                        "通用", "一般", "General") == null) {
                    return;
                }
            }
        }

        boolean isDarkMode = StyleUtils.isDarkMode(context);

        LinearLayout settingsItemRootLLayout = new LinearLayout(context);
        settingsItemRootLLayout.setOrientation(LinearLayout.VERTICAL);
        settingsItemRootLLayout.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (versionCode >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_20) {
            // 减少页面跳动
            settingsItemRootLLayout.setPadding(0, 0, 0, 0);
        } else {
            settingsItemRootLLayout.setPadding(0, DpUtils.dip2px(context, 20), 0, 0);
        }

        LinearLayout settingsItemLinearLayout = new LinearLayout(context);
        settingsItemLinearLayout.setOrientation(LinearLayout.VERTICAL);

        settingsItemLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        LinearLayout itemHlinearLayout = new LinearLayout(context);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);

        itemHlinearLayout.setBackground(new XDrawable.Builder()
                .defaultColor(isDarkMode ? 0xFF191919 : Color.WHITE)
                .pressedColor(isDarkMode ? 0xFF1D1D1D : 0xFFE5E5E5)
                .create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(context).showInDialog());

        int defHPadding = DpUtils.dip2px(context, 15);

        TextView itemNameText = new TextView(context);
        itemNameText.setTextColor(isDarkMode ? 0xFFD3D3D3 : 0xFF353535);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtils.dip2px(context, 16), 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(context);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(isDarkMode ? 0xFF656565 : 0xFF999999);

        //try use WeChat style
        try {
            View generalView = ViewUtils.findViewByText(itemView, "通用", "一般", "General", "服务管理", "服務管理", "Manage Services");
            L.d("generalView", generalView);
            if (generalView instanceof TextView) {
                TextView generalTextView = (TextView) generalView;
                float scale = itemNameText.getTextSize() / generalTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, generalTextView.getTextSize());

                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                View generalItemView;
                if (versionCode >= 1380) { //7.0.0
                    generalItemView = (View) generalView.getParent().getParent().getParent().getParent().getParent();
                } else {
                    generalItemView = (View) generalView.getParent().getParent().getParent().getParent().getParent().getParent();
                }
                if (generalItemView != null) {
                    Drawable background = generalItemView.getBackground();
                    if (background != null) {
                        Drawable.ConstantState constantState = background.getConstantState();
                        if (constantState != null) {
                            itemHlinearLayout.setBackground(constantState.newDrawable());
                        }
                    }
                }
                itemNameText.setTextColor(generalTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (versionCode >= 1380) { //7.0.0
            View lineView = new View(context);
            lineView.setBackgroundColor(isDarkMode ? 0xFF2E2E2E : 0xFFD5D5D5);
            settingsItemLinearLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 55)));
        } else {
            settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 50)));
        }

        settingsItemRootLLayout.addView(settingsItemLinearLayout);
        settingsItemRootLLayout.setTag(BuildConfig.APPLICATION_ID);

        itemView.addHeaderView(settingsItemRootLLayout);
    }
}
