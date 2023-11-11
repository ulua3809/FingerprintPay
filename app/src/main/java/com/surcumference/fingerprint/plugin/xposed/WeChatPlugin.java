package com.surcumference.fingerprint.plugin.xposed;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.UserHandle;

import androidx.annotation.Keep;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.plugin.PluginFactory;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
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

public class WeChatPlugin {

    @Keep
    public void main(final Application application, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            PluginApp.setup(PluginType.Xposed, PluginTarget.WeChat);
            Toaster.init(application);
            Umeng.init(application);
            XposedLogNPEBugFixer.fix();
            UpdateFactory.lazyUpdateWhenActivityAlive();
            IAppPlugin plugin = PluginFactory.loadPlugin(application, Constant.PACKAGE_NAME_WECHAT);
            //for multi user
            if (!Tools.isCurrentUserOwner(application)) {
                XposedHelpers.findAndHookMethod(UserHandle.class, "getUserId", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (plugin.getMockCurrentUser()) {
                            param.setResult(0);
                        }
                    }
                });
            }
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    plugin.onActivityResumed((Activity) param.thisObject);
                }
            });

            XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    plugin.onActivityPaused((Activity) param.thisObject);
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }
}
