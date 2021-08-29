package com.surcumference.fingerprint.network.updateCheck;

import com.surcumference.fingerprint.network.inf.IUpdateCheck;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.util.Task;

/**
 * Created by Jason on 2017/9/9.
 */

public abstract class BaseUpdateChecker implements IUpdateCheck, UpdateResultListener {

    private UpdateResultListener mResultListener;

    public BaseUpdateChecker(UpdateResultListener listener) {
        mResultListener = listener;
    }

    @Override
    public void onNoUpdate() {
        Task.onMain(() -> {
            UpdateResultListener listener = mResultListener;
            if (listener == null) {
                return;
            }
            listener.onNoUpdate();
        });
    }

    @Override
    public void onNetErr() {
        Task.onMain(() -> {
            UpdateResultListener listener = mResultListener;
            if (listener == null) {
                return;
            }
            listener.onNetErr();
        });
    }

    @Override
    public void onHasUpdate(final String version, final String content, final String pageUrl, String downloadUrl) {
        Task.onMain(() -> {
            UpdateResultListener listener = mResultListener;
            if (listener == null) {
                return;
            }
            listener.onHasUpdate(version, content, pageUrl, downloadUrl);
        });
    }
}
