package com.yyxx.wechatfp.xposed;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.util.log.L;
import com.yyxx.wechatfp.xposed.loader.XposedPluginLoader;
import com.yyxx.wechatfp.xposed.plugin.XposedAlipayPlugin;
import com.yyxx.wechatfp.xposed.plugin.XposedQQPlugin;
import com.yyxx.wechatfp.xposed.plugin.XposedTaobaoPlugin;
import com.yyxx.wechatfp.xposed.plugin.XposedWeChatPlugin;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.yyxx.wechatfp.Constant.PACKAGE_NAME_ALIPAY;
import static com.yyxx.wechatfp.Constant.PACKAGE_NAME_QQ;
import static com.yyxx.wechatfp.Constant.PACKAGE_NAME_TAOBAO;
import static com.yyxx.wechatfp.Constant.PACKAGE_NAME_WECHAT;


public class WalletBaseUI implements IXposedHookZygoteInit, IXposedHookLoadPackage {


    public void initZygote(StartupParam startupParam) throws Throwable {
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (PACKAGE_NAME_WECHAT.equals(lpparam.packageName)) {
            initWechat(lpparam);
        } else if (PACKAGE_NAME_ALIPAY.equals(lpparam.packageName)) {
            initAlipay(lpparam);
        } else if (PACKAGE_NAME_TAOBAO.equals(lpparam.packageName)) {
            initTaobao(lpparam);
        } else if (PACKAGE_NAME_QQ.equals(lpparam.packageName)) {
            initQQ(lpparam);
        }
        initGeneric(lpparam);
    }

    private void initWechat(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @TargetApi(21)
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                Context context = (Context) param.args[0];
                XposedPluginLoader.load(XposedWeChatPlugin.class, context, lpparam);
            }
        });
    }

    private void initAlipay(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            private boolean mCalled = false;
            @TargetApi(21)
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                if (mCalled == false) {
                    mCalled = true;
                    Context context = (Context) param.args[0];
                    XposedPluginLoader.load(XposedAlipayPlugin.class, context, lpparam);
                }
            }
        });
    }

    private void initTaobao(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            //受Atlas影响Application onCreate入口只需执行一次即可
            private boolean mCalled = false;
            @TargetApi(21)
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                if (mCalled == false) {
                    mCalled = true;
                    Context context = (Context) param.args[0];
                    if (context == null) {
                        L.d("context eq null what the hell.");
                        return;
                    }
                    XposedPluginLoader.load(XposedTaobaoPlugin.class, context, lpparam);
                }
            }
        });
    }

    private void initQQ(final LoadPackageParam lpparam) {
        if (!lpparam.isFirstApplication) {
            return;
        }
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            private boolean mCalled = false;
            @TargetApi(21)
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                if (mCalled == false) {
                    mCalled = true;
                    Context context = (Context) param.args[0];
                    XposedPluginLoader.load(XposedQQPlugin.class, context, lpparam);
                }
            }
        });
    }

    private void initGeneric(final LoadPackageParam lpparam) {
        //for multi user
        if ("android".equals(lpparam.processName) || PACKAGE_NAME_WECHAT.equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod(ActivityManager.class, "checkComponentPermission", String.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String permission = (String) param.args[0];
                    if (TextUtils.isEmpty(permission)) {
                        return;
                    }
                    if (!permission.contains("MANAGE_USERS")) {
                        return;
                    }
                    param.setResult(PackageManager.PERMISSION_GRANTED);
                }
            });
        }
    }
}