package com.surcumference.fingerprint.plugin.magisk;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.Keep;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.plugin.WeChatBasePlugin;
import com.surcumference.fingerprint.util.ActivityLifecycleCallbacks;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.L;


/**
 * Created by Jason on 2017/9/8.
 */
public class WeChatPlugin extends WeChatBasePlugin {

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
        WeChatPlugin plugin = new WeChatPlugin();

        Task.onMain(1000, ()-> Umeng.init(application));

        UpdateFactory.lazyUpdateWhenActivityAlive();
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                plugin.onActivityResumed(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                plugin.onActivityPaused(activity);
            }
        });
    }
}
