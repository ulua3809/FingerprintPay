package com.surcumference.fingerprint.plugin.magisk;

import android.app.Application;

import androidx.annotation.Keep;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.update.UpdateFactory;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.plugin.PluginFactory;
import com.surcumference.fingerprint.plugin.inf.IAppPlugin;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.L;


/**
 * Created by Jason on 2017/9/8.
 */
public class WeChatPlugin {

    /**
     * >= 4.2.0
     */
    @Keep
    public static void main(String niceName, String pluginTypeName) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        PluginApp.setup(pluginTypeName, PluginTarget.WeChat);
        Task.onApplicationReady(WeChatPlugin::init);
    }

    /**
     * <= 4.0.1
     * Note: 可能会导致降级后还显示上一个版本
     */
    @Keep
    @Deprecated
    public static void main(String niceName) {
        main(niceName, PluginType.Riru.name());
    }

    public static void init() {
        Application application = ApplicationUtils.getApplication();
        IAppPlugin plugin = PluginFactory.loadPlugin(application, Constant.PACKAGE_NAME_WECHAT);
        Toaster.init(application);
        Task.onMain(1000, ()-> Umeng.init(application));

        UpdateFactory.lazyUpdateWhenActivityAlive();
        application.registerActivityLifecycleCallbacks(plugin);
    }
}
