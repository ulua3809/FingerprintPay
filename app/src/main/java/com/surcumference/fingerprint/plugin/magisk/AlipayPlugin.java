package com.surcumference.fingerprint.plugin.magisk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Keep;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.plugin.AlipayBasePlugin;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.util.ActivityLifecycleCallbacks;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.L;

/**
 * Created by Jason on 2017/9/8.
 */
public class AlipayPlugin extends AlipayBasePlugin {

    @Keep
    public static void main(String appDataDir) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        Task.onApplicationReady(AlipayPlugin::init);
    }

    public static void init() {
        PluginApp.setup(PluginType.Magisk, PluginTarget.Alipay);
        Application application = ApplicationUtils.getApplication();
        AlipayPlugin plugin = new AlipayPlugin();
        Task.onMain(1000, ()-> Umeng.init(application));

        UpdateFactory.lazyUpdateWhenActivityAlive();
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                plugin.onActivityCreated(activity);
            }
        });
    }
}
