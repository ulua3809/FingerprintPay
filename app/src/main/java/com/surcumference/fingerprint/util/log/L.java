package com.surcumference.fingerprint.util.log;

import android.util.Log;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.util.log.handler.GenericLog;
import com.surcumference.fingerprint.util.log.handler.XposedLog;
import com.surcumference.fingerprint.util.log.inf.ILog;

/**
 * Created by Jason on 2017/9/10.
 */

public class L {

    private static final ILog sILog;

    private static final String LOG_TAG = BuildConfig.APP_PRODUCT_NAME;

    static {
        ILog iLog;
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            iLog = new XposedLog();
        } catch (Exception | Error ignore) {
            iLog = new GenericLog();
        }
        sILog = iLog;
    }


    public static void d(Object... arg) {
        final String log = arg2string(arg);
        if (log != null) {
            sILog.debug(LOG_TAG + getTraceTag(), log);
        }
    }

    public static void e(Object... arg) {
        final String log = arg2string(arg);
        if (log != null) {
            sILog.error("tag:" + LOG_TAG + " ver:" + BuildConfig.VERSION_NAME + " " + getTraceTag(), log);
        }
    }

    private static String arg2string(Object[] arg) {
        StringBuilder sb = new StringBuilder();
        try {

            if (arg != null)
                for (Object o : arg)
                    try {
                        if (o instanceof Exception) {
                            sb.append(Log.getStackTraceString(((Exception) (o))));
                            if (arg.length != 1)
                                sb.append("\t");
                        } else if (o instanceof Error) {
                            sb.append(Log.getStackTraceString(((Error) (o))));
                            if (arg.length != 1)
                                sb.append("\t");
                        } else {
                            sb.append(String.valueOf(o));
                            if (arg.length != 1)
                                sb.append("\t");
                        }
                    } catch (Exception e) {
                    }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.gc();
        }
        if (sb.length() < 1)
            return null;
        else
            return sb.toString();
    }

    private static String getTraceTag() {
        StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[4];

        String className = stackTrace.getClassName();
        className = className.replaceAll("\\$.+", "");
        return " [" + stackTrace.getMethodName() + "](" + className.substring(className.lastIndexOf('.') + 1) + ".java:" + stackTrace.getLineNumber() + ")";
    }
}
