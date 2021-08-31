package com.surcumference.fingerprint.network.inf;

import com.surcumference.fingerprint.bean.UpdateInfo;

/**
 * Created by Jason on 2017/9/9.
 */

public interface UpdateResultListener {

    void onNoUpdate();
    void onNetErr();
    void onHasUpdate(UpdateInfo updateInfo);
}
