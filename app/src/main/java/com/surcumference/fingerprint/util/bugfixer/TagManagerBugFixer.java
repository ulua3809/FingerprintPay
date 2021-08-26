package com.surcumference.fingerprint.util.bugfixer;

/**
 * Created by Jason on 2017/11/12.
 */

public class TagManagerBugFixer {

    public static void fix() {
        // for catching app global unhandled exceptions
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new MyHandler(defaultHandler));
    }

    private static class MyHandler implements Thread.UncaughtExceptionHandler {

        private final Thread.UncaughtExceptionHandler defaultHandler;

        MyHandler(Thread.UncaughtExceptionHandler defaultHandler) {
            this.defaultHandler = defaultHandler;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            String classpath = null;
            if (ex != null && ex.getStackTrace().length > 0) {
                classpath = ex.getStackTrace()[0].toString();
            }
            if (classpath != null &&
                    ex.getMessage().contains("Results have already been set") &&
                    classpath.contains("com.google.android.gms.tagmanager") ) {
                // ignore
            } else {
                // run your default handler
                defaultHandler.uncaughtException(thread, ex);
            }
        }
    }
}
