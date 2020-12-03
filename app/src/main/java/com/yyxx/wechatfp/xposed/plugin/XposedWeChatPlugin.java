package com.yyxx.wechatfp.xposed.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
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
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.Constant;
import com.yyxx.wechatfp.Lang;
import com.yyxx.wechatfp.R;
import com.yyxx.wechatfp.network.updateCheck.UpdateFactory;
import com.yyxx.wechatfp.util.ActivityViewObserver;
import com.yyxx.wechatfp.util.BlackListUtils;
import com.yyxx.wechatfp.util.Config;
import com.yyxx.wechatfp.util.DpUtil;
import com.yyxx.wechatfp.util.ImageUtil;
import com.yyxx.wechatfp.util.StyleUtil;
import com.yyxx.wechatfp.util.Task;
import com.yyxx.wechatfp.util.Tools;
import com.yyxx.wechatfp.util.Umeng;
import com.yyxx.wechatfp.util.ViewUtil;
import com.yyxx.wechatfp.util.bugfixer.xposed.XposedLogNPEBugFixer;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.view.SettingsView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedWeChatPlugin {

    private FingerprintIdentify mFingerprintIdentify;
    private Activity mCurrentActivity;
    private boolean mMockCurrentUser = false;
    private int mWeChatVersionCode;
    private ActivityViewObserver mActivityViewObserver;
    private WeakHashMap<View, View.OnAttachStateChangeListener> mView2OnAttachStateChangeListenerMap = new WeakHashMap<>();

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            Umeng.init(context);
            XposedLogNPEBugFixer.fix();
            mWeChatVersionCode = getWeChatVersionCode(context);
            L.d("WeChat Version code:" + mWeChatVersionCode);
            //for multi user
            if (!Tools.isCurrentUserOwner(context)) {
                XposedHelpers.findAndHookMethod(UserHandle.class, "getUserId", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mMockCurrentUser) {
                            param.setResult(0);
                        }
                    }
                });
            }
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    boolean firstStartUp = mCurrentActivity == null;
                    Activity activity = (Activity) param.thisObject;
                    L.d("Activity onResume =", activity);
                    mCurrentActivity = activity;
                    if (firstStartUp) {
                        Task.onMain(6000L, () -> UpdateFactory.doUpdateCheck(activity));
                    }
                    final String activityClzName = activity.getClass().getName();
                    if (activityClzName.contains("com.tencent.mm.plugin.setting.ui.setting.SettingsUI")
                            || activityClzName.contains("com.tencent.mm.plugin.wallet.pwd.ui.WalletPasswordSettingUI")) {
                        Task.onMain(100, () -> doSettingsMenuInject(activity));
                    } else if (activityClzName.contains(".WalletPayUI")
                        || activityClzName.contains(".UIPageFragmentActivity")) {
                        stopAndRemoveCurrentActivityViewObserver();
                        ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity, ".EditHintPasswdView");
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
            });

            XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    L.d("Activity onPause =", activity);
                    final String activityClzName = activity.getClass().getName();
                    if (activityClzName.contains(".WalletPayUI")) {
                        stopAndRemoveCurrentActivityViewObserver();
                        onPayDialogDismiss(activity);
                    }
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }

    private void onPayDialogShown(ViewGroup rootView) {
        L.d("PayDialog show");
        Context context = rootView.getContext();
        if (Config.from(context).isOn()) {
            PayDialog payDialogView = PayDialog.findFrom(rootView);
            L.d(payDialogView);
            if (payDialogView == null) {
                notifyCurrentVersionUnSupport(context);
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
                final Bitmap bitmap = ImageUtil.base64ToBitmap(Constant.ICON_FINGER_PRINT_WECHAT_BASE64);
                fingerprintImageView.setImageBitmap(bitmap);
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
                    mInputEditText.setText(pwd);
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

    private void onPayDialogDismiss(Context context) {
        L.d("PayDialog dismiss");
        if (Config.from(context).isOn()) {
            FingerprintIdentify fingerPrintIdentify = mFingerprintIdentify;
            if (fingerPrintIdentify != null) {
                fingerPrintIdentify.cancelIdentify();
            }
            mMockCurrentUser = false;
        }
    }

    private void stopAndRemoveCurrentActivityViewObserver() {
        ActivityViewObserver activityViewObserver = mActivityViewObserver;
        if (activityViewObserver != null) {
            activityViewObserver.stop();
            mActivityViewObserver = null;
        }
    }

    private void doSettingsMenuInject(final Activity activity) {
        ListView itemView = (ListView)ViewUtil.findViewByName(activity, "android", "list");
        if (ViewUtil.findViewByText(itemView, Lang.getString(R.id.app_settings_name)) != null
                || isHeaderViewExistsFallback(itemView)) {
            return;
        }

        boolean isDarkMode = StyleUtil.isDarkMode(activity);

        LinearLayout settingsItemRootLLayout = new LinearLayout(activity);
        settingsItemRootLLayout.setOrientation(LinearLayout.VERTICAL);
        settingsItemRootLLayout.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        settingsItemRootLLayout.setPadding(0, DpUtil.dip2px(activity, 20), 0, 0);

        LinearLayout settingsItemLinearLayout = new LinearLayout(activity);
        settingsItemLinearLayout.setOrientation(LinearLayout.VERTICAL);

        settingsItemLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(ViewUtil.genBackgroundDefaultDrawable(isDarkMode ? 0xFF191919 : Color.WHITE, isDarkMode ? 0xFF1D1D1D : 0xFFE5E5E5));
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtil.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        itemNameText.setTextColor(isDarkMode ? 0xFFD3D3D3 : 0xFF353535);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtil.dip2px(activity, 16), 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtil.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtil.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(isDarkMode ? 0xFF656565 : 0xFF999999);

        //try use WeChat style
        try {
            View generalView = ViewUtil.findViewByText(itemView, "通用", "一般", "General", "服务管理", "服務管理", "Manage Services");
            L.d("generalView", generalView);
            if (generalView instanceof TextView) {
                TextView generalTextView = (TextView) generalView;
                float scale = itemNameText.getTextSize() / generalTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, generalTextView.getTextSize());

                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                View generalItemView;
                if (mWeChatVersionCode >= 1380) { //7.0.0
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

        if (mWeChatVersionCode >= 1380) { //7.0.0
            View lineView = new View(activity);
            lineView.setBackgroundColor(isDarkMode ? 0xFF2E2E2E : 0xFFD5D5D5);
            settingsItemLinearLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtil.dip2px(activity, 55)));
        } else {
            settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtil.dip2px(activity, 50)));
        }

        settingsItemRootLLayout.addView(settingsItemLinearLayout);
        settingsItemRootLLayout.setTag(BuildConfig.APPLICATION_ID);

        itemView.addHeaderView(settingsItemRootLLayout);

    }

    private boolean isHeaderViewExistsFallback(ListView listView) {
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

    public synchronized void initFingerPrintLock(Context context, Runnable onSuccessUnlockRunnable) {
        mMockCurrentUser = true;
        mFingerprintIdentify = new FingerprintIdentify(context.getApplicationContext(), exception -> {
            if (exception instanceof SsdkUnsupportedException) {
                return;
            }
            L.e("fingerprint", exception);
        });
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_match), Toast.LENGTH_SHORT).show();
                    L.d("指纹识别成功");
                    onSuccessUnlockRunnable.run();
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
                    L.d("多次尝试错误，请确认指纹");
                    Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_retry_ended), Toast.LENGTH_SHORT).show();
                    mMockCurrentUser = false;
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_unlock_reboot), Toast.LENGTH_SHORT).show();
                    mMockCurrentUser = false;
                }
            });
        } else {
            L.d("系统指纹功能未启用");
            Toast.makeText(context, Lang.getString(R.id.toast_fingerprint_not_enable), Toast.LENGTH_SHORT).show();
            mMockCurrentUser = false;
        }
    }

    private static class PayDialog {

        private ViewGroup passwordLayout;
        private EditText inputEditText;
        private View keyboardView;
        @Nullable
        private TextView usePasswordText;
        @Nullable
        private TextView titleTextView;

        @Nullable
        public static PayDialog findFrom(ViewGroup rootView) {
            try {
                PayDialog payDialog = new PayDialog();

                List<View> childViews = new ArrayList<>();
                ViewUtil.getChildViews(rootView, childViews);
                for (View view : childViews) {
                    if (view == null) {
                        continue;
                    }
                    if (view.getClass().getName().endsWith(".EditHintPasswdView")) {
                        L.d("mPasswordLayout:" + view);
                        if (view instanceof ViewGroup) {
                            payDialog.passwordLayout = (ViewGroup)view;
                        }
                    } else if (view.getClass().getName().endsWith(".TenpaySecureEditText")) {
                        L.d("密码输入框:" + view);
                        if (view instanceof EditText) {
                            payDialog.inputEditText = (EditText)view;
                        }
                    } else if (view.getClass().getName().endsWith(".MyKeyboardWindow")) {
                        L.d("密码键盘:" + view);
                        if (view.getParent() != null) {
                            payDialog.keyboardView = (View)view.getParent();
                        }
                    }
                }

                if (payDialog.passwordLayout == null) {
                    Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat passwordLayout NOT FOUND]  " + ViewUtil.viewsDesc(childViews));
                    return null;
                }

                if (payDialog.inputEditText == null) {
                    Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat inputEditText NOT FOUND]  " + ViewUtil.viewsDesc(childViews));
                    return null;
                }

                if (payDialog.keyboardView == null) {
                    Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat keyboardView NOT FOUND]  " + ViewUtil.viewsDesc(childViews));
                    return null;
                }

                payDialog.usePasswordText = (TextView)ViewUtil.findViewByText(rootView,
                        "使用密码", "使用密碼", "Password",
                        "使用指纹", "使用指紋", "Fingerprint");
                L.d("payDialog.usePasswordText", payDialog.usePasswordText); // 6.5.16 app:id/dh0
                if (payDialog.usePasswordText == null) {
                    Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat usePasswordText NOT FOUND]  " + ViewUtil.viewsDesc(childViews));
                }

                payDialog.titleTextView = (TextView)ViewUtil.findViewByText(rootView,
                        "请验证指纹", "請驗證指紋", "Verify fingerprint",
                        "请输入支付密码", "請輸入付款密碼", "Enter payment password");
                L.d("payDialog.titleTextView", payDialog.titleTextView); // 6.5.16 app:id/dgz
                if (payDialog.titleTextView == null) {
                    Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat titleTextView NOT FOUND]  " + ViewUtil.viewsDesc(childViews));
                }
                return payDialog;
            } catch (Exception e) {
                L.e(e);
            }
            return null;
        }

        @Override
        public String toString() {
            return "PayDialog{" +
                    "passwordLayout=" + passwordLayout +
                    ", inputEditText=" + inputEditText +
                    ", keyboardView=" + keyboardView +
                    ", usePasswordText=" + usePasswordText +
                    ", titleTextView=" + titleTextView +
                    '}';
        }
    }

    private static void notifyCurrentVersionUnSupport(final Context context) {
        Task.onMain(()->{
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PACKAGE_NAME_WECHAT, 0);
                int versionCode = packageInfo.versionCode;
                String versionName = packageInfo.versionName;
                Toast.makeText(context, "当前版本:" + versionName + "." + versionCode + "不支持", Toast.LENGTH_LONG).show();
                L.d("当前版本:" + versionName + "." + versionCode + "不支持");
            } catch (Exception e) {
                L.e(e);
            }
        });
    }

    private static int getWeChatVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PACKAGE_NAME_WECHAT, 0);
            int versionCode = packageInfo.versionCode;
            return versionCode;
        } catch (Exception e) {
            L.e(e);
        }
        return 0;
    }

    private static boolean verifyPayDialog(Class targetClass) {
        if (!targetClass.getName().startsWith("com.tencent.mm.plugin.wallet_core.ui.")) {
            return false;
        }
        String method = Arrays.toString(targetClass.getDeclaredMethods());
        if (!method.contains("Bankcard")) {
            return false;
        }
        String field = Arrays.toString(targetClass.getDeclaredFields());
        if (!field.contains("MyKeyboardWindow")) {
            return false;
        }
        if (!field.contains("EditHintPasswdView")) {
            return false;
        }
        if (!field.contains("Bankcard")) {
            return false;
        }
        return true;
    }
}
