package com.surcumference.fingerprint.bean;

import android.support.annotation.IdRes;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;

public enum PluginTarget {
    QQ(R.id.settings_title_qq),
    WeChat(R.id.settings_title_wechat),
    Alipay(R.id.settings_title_alipay),
    Taobao(R.id.settings_title_taobao);

    @IdRes
    private int mAppNameRes;

    PluginTarget(@IdRes int appNameRes) {
        mAppNameRes = appNameRes;
    }

    public String getAppName() {
        return Lang.getString(mAppNameRes);
    }
}