package com.surcumference.fingerprint.util.log.handler;

import android.util.Log;

import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.log.inf.ILog;

/**
 * Created by Jason on 2017/9/10.
 */

public class GenericLog implements ILog {
    @Override
    public void debug(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void error(String tag, String msg) {
        Log.e(tag, msg);
        Umeng.reportError(tag + " " + msg);
    }
}
