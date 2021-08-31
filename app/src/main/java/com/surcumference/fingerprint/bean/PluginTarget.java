package com.surcumference.fingerprint.bean;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;

public enum PluginTarget {
    QQ(Lang.getString(R.id.settings_title_qq)),
    WeChat(Lang.getString(R.id.settings_title_wechat)),
    Alipay(Lang.getString(R.id.settings_title_alipay)),
    Taobao(Lang.getString(R.id.settings_title_taobao));

    private String mAppName;

    PluginTarget(String appName) {
        mAppName = appName;
    }

    public String getAppName() {
        return mAppName;
    }
}