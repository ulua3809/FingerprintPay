package com.surcumference.fingerprint.network.inf;

/**
 * Created by Jason on 2017/9/9.
 */

public interface UpdateResultListener {

    void onNoUpdate();
    void onNetErr();
    void onHasUpdate(String version, String content, String pageUrl, String downloadUrl);
}
