package com.surcumference.fingerprint.plugin.xposed;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.UserHandle;
import android.support.annotation.Keep;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.plugin.WeChatBasePlugin;
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

public class WeChatPlugin extends WeChatBasePlugin {

    private Activity mCurrentActivity;

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            Umeng.init(context);
            XposedLogNPEBugFixer.fix();
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
                    onActivityResumed(activity);
                }
            });

            XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
                @TargetApi(21)
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    onActivityPaused(activity);
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }
}
