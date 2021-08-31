package com.surcumference.fingerprint.plugin.xposed;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.text.TextUtils;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.plugin.QQBasePlugin;
import com.surcumference.fingerprint.util.FileUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.Tools;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.bugfixer.xposed.XposedLogNPEBugFixer;
import com.surcumference.fingerprint.util.log.L;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


/**
 * Created by Jason on 2017/9/8.
 */

public class QQPlugin extends QQBasePlugin {

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.QQ);
            /**
             * FIX java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.Object java.lang.ref.WeakReference.get()' on a null object reference
             *     at com.tencent.mqq.shared_file_accessor.n.<init>(Unknown Source)
             *     at com.tencent.mqq.shared_file_accessor.SharedPreferencesProxyManager.getProxy(Unknown Source)
             *     at com.tencent.common.app.BaseApplicationImpl.getSharedPreferences(ProGuard:474)
             *     at com.tencent.common.app.QFixApplicationImpl.getSharedPreferences(ProGuard:247)
             *     at com.umeng.analytics.pro.ba.a(PreferenceWrapper.java:24)
             *     at com.umeng.analytics.pro.cc.f(StoreHelper.java:127)
             *     at com.umeng.analytics.AnalyticsConfig.getVerticalType(AnalyticsConfig.java:133)
             */
            Task.onMain(1000, ()-> Umeng.init(context));
            XposedLogNPEBugFixer.fix();
            String niceName = FileUtils.getCmdLineContentByPid(android.os.Process.myPid());
            if (!TextUtils.isEmpty(niceName)
                    && !niceName.contains(":")) {
                UpdateFactory.lazyUpdateWhenActivityAlive();
            }
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
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {

                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    onActivityCreated((Activity) param.thisObject);
                }
            });

            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {

                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    onActivityResumed((Activity) param.thisObject);
                }
            });
            XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {

                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    onActivityPaused((Activity) param.thisObject);
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }
}
