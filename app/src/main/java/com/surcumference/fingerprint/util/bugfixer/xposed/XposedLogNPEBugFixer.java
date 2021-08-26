package com.surcumference.fingerprint.util.bugfixer.xposed;

import com.surcumference.fingerprint.util.log.L;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by Jason on 2017/11/12.
 */

public class XposedLogNPEBugFixer {

    /**
     *
     java.lang.NullPointerException: println needs a message
     at android.util.Log.println_native(Native Method)
     at android.util.Log.i(Log.java:160)
     at de.robv.android.xposed.XposedBridge.log(XposedBridge.java:147)
     at com.fkzhang.qqxposedhooks.h.j$b.ʻ(Unknown Source)
     at com.fkzhang.qqxposedhooks.h.j$b.ʻ(Unknown Source)
     at com.fkzhang.qqxposedhooks.h.j$b.doInBackground(Unknown Source)
     at android.os.AsyncTask$2.call(AsyncTask.java:295)
     at java.util.concurrent.FutureTask.run(FutureTask.java:237)
     at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1113)
     at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:588)
     at java.lang.Thread.run(Thread.java:818)
     */
    public static void fix() {
        try {
            XposedHelpers.findAndHookMethod(XposedBridge.class, "log", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0] == null) {
                        param.args[0] = "xx";
                    }
                    super.beforeHookedMethod(param);
                }
            });
        } catch (Exception e) {
            L.e(e);
        }
    }
}
