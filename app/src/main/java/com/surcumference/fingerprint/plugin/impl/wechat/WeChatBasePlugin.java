package com.surcumference.fingerprint.plugin.impl.wechat;

import static com.surcumference.fingerprint.Constant.PACKAGE_NAME_WECHAT;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.DigitPasswordKeyPadInfo;
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
import com.surcumference.fingerprint.util.FragmentObserver;
import com.surcumference.fingerprint.util.ImageUtils;
import com.surcumference.fingerprint.util.NotifyUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.WeChatVersionControl;
import com.surcumference.fingerprint.util.XBiometricIdentify;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.util.paydialog.WeChatPayDialog;
import com.surcumference.fingerprint.view.SettingsView;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class WeChatBasePlugin implements IAppPlugin, IMockCurrentUser {

    private WeakHashMap<View, View.OnAttachStateChangeListener> mView2OnAttachStateChangeListenerMap = new WeakHashMap<>();
    protected boolean mMockCurrentUser = false;
    protected XBiometricIdentify mFingerprintIdentify;
    private FragmentObserver mFragmentObserver;

    private int mWeChatVersionCode = 0;

    @Override
    public int getVersionCode(Context context) {
        if (mWeChatVersionCode != 0) {
            return mWeChatVersionCode;
        }
        mWeChatVersionCode = ApplicationUtils.getPackageVersionCode(context, PACKAGE_NAME_WECHAT);
        return mWeChatVersionCode;
    }

    protected synchronized void initFingerPrintLock(Context context, Config config,
                                                    boolean smallPayDialogFloating, String passwordEncrypted,
                                                    OnFingerprintVerificationOKListener onSuccessUnlockCallback,
                                                    final Runnable onFailureUnlockCallback) {
        cancelFingerprintIdentify();
        mFingerprintIdentify = new BizBiometricIdentify(context)
                .withMockCurrentUserCallback(this)
                .decryptPasscode(passwordEncrypted, new BizBiometricIdentify.IdentifyListener() {

                    @Override
                    public void onDecryptionSuccess(BizBiometricIdentify identify, @NonNull String decryptedContent) {
                        super.onDecryptionSuccess(identify, decryptedContent);
                        onSuccessUnlockCallback.onFingerprintVerificationOK(decryptedContent);
                    }

                    @Override
                    public void onFailed(BizBiometricIdentify target, FingerprintIdentifyFailInfo failInfo) {
                        super.onFailed(target, failInfo);
                        onFailureUnlockCallback.run();
                    }
                });
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

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivityResumed(Activity activity) {
        L.d("Activity onResume =", activity);
        final String activityClzName = activity.getClass().getName();
        if (activityClzName.contains("com.tencent.mm.plugin.setting.ui.setting.SettingsUI")
                || activityClzName.contains("com.tencent.mm.plugin.wallet.pwd.ui.WalletPasswordSettingUI")
                || activityClzName.contains("com.tencent.mm.ui.vas.VASCommonActivity") /** 8.0.18 */) {
            Task.onMain(100, () -> doSettingsMenuInject(activity));
        } else if (getVersionCode(activity) >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_20 && activityClzName.contains("com.tencent.mm.ui.LauncherUI")) {
            startFragmentObserver(activity);
        } else if (activityClzName.contains(".WalletPayUI")
                || activityClzName.contains(".UIPageFragmentActivity")) {
            ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity);
            activityViewObserver.setViewIdentifyType(".EditHintPasswdView");
            ActivityViewObserverHolder.start(ActivityViewObserverHolder.Key.WeChatPayView,  activityViewObserver,
                    100, new ActivityViewObserver.IActivityViewListener() {
                @Override
                public void onViewFounded(ActivityViewObserver observer, View view) {
                    ActivityViewObserver.IActivityViewListener l = this;
                    ActivityViewObserverHolder.stop(observer);
                    L.d("onViewFounded:", view, " rootView: ", view.getRootView());
                    view.postDelayed(() -> onPayDialogShown((ViewGroup) view.getRootView()), 100);
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
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        //Xposed not hooked yet!
    }

    @Override
    public void onActivityPaused(Activity activity) {
        try {
            L.d("Activity onPause =", activity);
            final String activityClzName = activity.getClass().getName();
            if (activityClzName.contains(".WalletPayUI")
                || activityClzName.contains(".UIPageFragmentActivity")) {
                ActivityViewObserverHolder.stop(ActivityViewObserverHolder.Key.WeChatPayView);
                onPayDialogDismiss(activity);
            } else if (getVersionCode(activity) >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_20 && activityClzName.contains("com.tencent.mm.ui.LauncherUI")) {
                stopFragmentObserver(activity);
            }
        } catch (Exception e) {
            L.e(e);
        }
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
        Config config = Config.from(context);
        if (!config.isOn()) {
            return;
        }
        String passwordEncrypted = config.getPasswordEncrypted();
        if (TextUtils.isEmpty(passwordEncrypted) || TextUtils.isEmpty(config.getPasswordIV())) {
            NotifyUtils.notifyBiometricIdentify(context, Lang.getString(R.id.toast_password_not_set_wechat));
            return;
        }

        int versionCode = getVersionCode(context);
        WeChatPayDialog payDialogView = WeChatPayDialog.findFrom(versionCode, rootView);
        L.d(payDialogView);
        if (payDialogView == null) {
            NotifyUtils.notifyVersionUnSupport(context, Constant.PACKAGE_NAME_WECHAT);
            return;
        }

        ViewGroup passwordLayout = payDialogView.passwordLayout;
        EditText mInputEditText = payDialogView.inputEditText;
        List<View> keyboardViews = payDialogView.keyboardViews;
        TextView usePasswordText = payDialogView.usePasswordText;
        TextView titleTextView = payDialogView.titleTextView;

        boolean smallPayDialogFloating = isSmallPayDialogFloating(passwordLayout);
        RelativeLayout fingerPrintLayout = new RelativeLayout(context);
        fingerPrintLayout.setTag("fingerPrintLayout");
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        fingerPrintLayout.setLayoutParams(layoutParams);

        fingerPrintLayout.setClipChildren(false);
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
        RelativeLayout.LayoutParams fingerprintImageViewLayoutParams = new RelativeLayout.LayoutParams(DpUtils.dip2px(context, 70), DpUtils.dip2px(context, 70));
        fingerprintImageViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        fingerprintImageViewLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        if (smallPayDialogFloating) {
            fingerprintImageViewLayoutParams.topMargin = DpUtils.dip2px(context, -14);
            fingerPrintLayout.addView(fingerprintImageView, fingerprintImageViewLayoutParams);
            fingerprintImageView.setVisibility(View.VISIBLE);
        } else {
            fingerprintImageViewLayoutParams.bottomMargin = DpUtils.dip2px(context, 180);
            fingerPrintLayout.addView(fingerprintImageView, fingerprintImageViewLayoutParams);
            fingerprintImageView.setVisibility(config.isShowFingerprintIcon() ? View.VISIBLE : View.GONE);
        }

        final Runnable switchToPasswordRunnable = ()-> {
            if (smallPayDialogFloating) {
                passwordLayout.removeView(fingerPrintLayout);
            } else {
                rootView.removeView(fingerPrintLayout);
            }
            mInputEditText.setVisibility(View.VISIBLE);
            keyboardViews.get(keyboardViews.size() - 1).setVisibility(View.VISIBLE);
            mInputEditText.requestFocus();
            mInputEditText.performClick();
            cancelFingerprintIdentify();
            mMockCurrentUser = false;
            if (titleTextView != null) {
                titleTextView.setText(Lang.getString(R.id.wechat_payview_password_title));
            }
            if (usePasswordText != null) {
                usePasswordText.setText(Lang.getString(R.id.wechat_payview_fingerprint_switch_text));
            }
        };

        final Runnable switchToFingerprintRunnable = ()-> {
            mInputEditText.setVisibility(View.GONE);
            for (View keyboardView : keyboardViews) {
                keyboardView.setVisibility(View.GONE);
            }
            if (smallPayDialogFloating) {
                View fingerPrintLayoutLast = passwordLayout.findViewWithTag("fingerPrintLayout");
                if (fingerPrintLayoutLast != null) {
                    passwordLayout.removeView(fingerPrintLayoutLast);
                }
                // 禁止修改, 会导致layoutListener 再次调用 switchToFingerprintRunnable
                // onPayDialogShown 调用 initFingerPrintLock
                // switchToFingerprintRunnable 调用 initFingerPrintLock 导致 onFailed 调用 switchToPasswordRunnable
                // switchToPasswordRunnable 调用 cancelFingerprintIdentify cancel 掉当前, 最终导致全部指纹识别取消
                // fingerPrintLayout.setVisibility(View.GONE);
                passwordLayout.addView(fingerPrintLayout);
                // ensure image icon visibility
                Task.onMain(1000, fingerPrintLayout::requestLayout);
                passwordLayout.setClipChildren(false);
                ((ViewGroup) passwordLayout.getParent()).setClipChildren(false);
                ((ViewGroup) passwordLayout.getParent().getParent()).setTop(((ViewGroup) passwordLayout.getParent().getParent()).getTop() + 200);
                ((ViewGroup) passwordLayout.getParent().getParent()).setClipChildren(false);
                ((ViewGroup) passwordLayout.getParent().getParent()).setBackgroundColor(Color.TRANSPARENT);
                ((ViewGroup) passwordLayout.getParent()).setBackgroundColor(Color.TRANSPARENT);
            } else {
                View fingerPrintLayoutLast = rootView.findViewWithTag("fingerPrintLayout");
                if (fingerPrintLayoutLast != null) {
                    rootView.removeView(fingerPrintLayoutLast);
                }
                rootView.addView(fingerPrintLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            initFingerPrintLock(context, config, smallPayDialogFloating, passwordEncrypted, (password)-> {
                BlackListUtils.applyIfNeeded(context);
                inputDigitalPassword(context, mInputEditText, password, keyboardViews, smallPayDialogFloating);
            }, switchToPasswordRunnable);
            if (titleTextView != null) {
                titleTextView.setText(Lang.getString(R.id.wechat_payview_fingerprint_title));
            }
            if (usePasswordText != null) {
                usePasswordText.setText(Lang.getString(R.id.wechat_payview_password_switch_text));
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
        if (titleTextView != null) {
            titleTextView.setOnTouchListener((view, motionEvent) -> {
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


        // 防止从选择支付页面返回时标题出错
        ViewTreeObserver.OnGlobalLayoutListener layoutListener = (ViewTreeObserver.OnGlobalLayoutListener) passwordLayout.getTag(R.id.tag_password_layout_listener);
        if (layoutListener != null) {
            passwordLayout.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        }
        layoutListener = () -> {
            AccessibilityNodeInfo nodeInfo = AccessibilityNodeInfo.obtain();
            passwordLayout.onInitializeAccessibilityNodeInfo(nodeInfo);
            if (nodeInfo.isVisibleToUser()) {
                if (fingerPrintLayout.getVisibility() != View.VISIBLE) {
                    fingerPrintLayout.setVisibility(View.VISIBLE);
                    switchToFingerprintRunnable.run();
                    // 防止从选择支付页面返回时标题出错
                    if (titleTextView != null) {
                        if (mInputEditText.getVisibility() == View.VISIBLE) {
                            titleTextView.setText(Lang.getString(R.id.wechat_payview_password_title));
                        } else {
                            titleTextView.setText(Lang.getString(R.id.wechat_payview_fingerprint_title));
                        }
                    }
                }
            } else {
                if (fingerPrintLayout.getVisibility() != View.GONE) {
                    fingerPrintLayout.setVisibility(View.GONE);
                    cancelFingerprintIdentify();
                }
            }
            nodeInfo.recycle();
        };
        passwordLayout.setTag(R.id.tag_password_layout_listener, layoutListener);
        passwordLayout.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    private void inputDigitalPassword(Context context, EditText inputEditText, String pwd,
                                      List<View> keyboardViews, boolean smallPayDialogFloating) {
        int versionCode = getVersionCode(context);
        if (versionCode >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_43) {
            DigitPasswordKeyPadInfo digitPasswordKeyPad = WeChatVersionControl.getDigitPasswordKeyPad(versionCode);
            inputEditText.getText().clear();
            View keyboardView = keyboardViews.get(0); //测了很多遍就是第一个
            // 在半高支付界面需要先激活inputEditText才能正常输入
            if (!smallPayDialogFloating) {
                ((ViewGroup)inputEditText.getParent().getParent()).setAlpha(0.01f);
                inputEditText.setVisibility(View.VISIBLE);
            }
            ViewGroup.LayoutParams keyboardViewParams = keyboardView.getLayoutParams();
            int keyboardViewHeight = keyboardViewParams.height;
            keyboardViewParams.height = 2;
            inputEditText.requestFocus();
            inputEditText.post(() -> {
                for (char c : pwd.toCharArray()) {
                    String[] keyIds = digitPasswordKeyPad.keys.get(String.valueOf(c));
                    if (keyIds == null) {
                        continue;
                    }
                    View digitView = ViewUtils.findViewByName(keyboardView, context.getPackageName(), keyIds);
                    if (digitView != null) {
                        ViewUtils.performActionClick(digitView);
                    }
                }
                // inputEditText.setVisibility(View.VISIBLE); 副作用反制
                keyboardView.post(() -> inputEditText.setVisibility(View.GONE));
                keyboardView.postDelayed(() -> {
                    ((ViewGroup)inputEditText.getParent().getParent()).setAlpha(1f);
                    keyboardViewParams.height = keyboardViewHeight;
                }, 1000);
            });
            return;
        }
        if (getVersionCode(context) >= Constant.WeChat.WECHAT_VERSION_CODE_8_0_18) {
            inputEditText.getText().clear();
            for (char c : pwd.toCharArray()) {
                inputEditText.append(String.valueOf(c));
            }
            return;
        }
        inputEditText.setText(pwd);
    }

    private boolean isSmallPayDialogFloating(ViewGroup passwordLayout) {
        ViewGroup floatRootView = ((ViewGroup) passwordLayout.getParent().getParent().getParent().getParent().getParent());
        int []location = new int[]{0,0};
        floatRootView.getLocationOnScreen(location);
        L.d("floatRootView", ViewUtils.getViewInfo(floatRootView));
        return location[0] > 0 || floatRootView.getChildCount() > 1;
    }

    protected void onPayDialogDismiss(Context context) {
        L.d("PayDialog dismiss");
        if (Config.from(context).isOn()) {
            cancelFingerprintIdentify();
            mMockCurrentUser = false;
        }
    }

    private void cancelFingerprintIdentify() {
        L.d("cancelFingerprintIdentify", new Exception());
        XBiometricIdentify fingerprintIdentify = mFingerprintIdentify;
        if (fingerprintIdentify == null) {
            return;
        }
        if (!fingerprintIdentify.fingerprintScanStateReady) {
            return;
        }
        fingerprintIdentify.cancelIdentify();
        mFingerprintIdentify = null;
    }

    protected void doSettingsMenuInject(final Activity activity) {
        doSettingsMenuInject(activity, activity.getWindow().getDecorView(), activity.getClass().getName());
    }

    protected void doSettingsMenuInject(Context context, View targetView, String targetClassName) {
        int versionCode = getVersionCode(context);
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
