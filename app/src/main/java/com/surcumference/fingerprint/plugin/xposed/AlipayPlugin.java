package com.surcumference.fingerprint.plugin.xposed;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.plugin.AlipayBasePlugin;
import com.surcumference.fingerprint.plugin.PluginApp;
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

public class AlipayPlugin extends AlipayBasePlugin {

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.Alipay);
            Umeng.init(context);
            XposedLogNPEBugFixer.fix();
            UpdateFactory.lazyUpdateWhenActivityAlive();
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
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }
}
