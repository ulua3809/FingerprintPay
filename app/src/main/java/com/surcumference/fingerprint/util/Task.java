package com.surcumference.fingerprint.util;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.surcumference.fingerprint.util.log.L;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jason on 2017/9/10.
 */

public class Task {

    private static class MainHandlerHolder {
        private static Handler handler = new Handler(Looper.getMainLooper());
    }

    public static void onMain(long msec, final Runnable runnable) {
        Runnable run = () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                L.d(e);
            }
        };
        MainHandlerHolder.handler.postDelayed(run, msec);
    }

    public static void onMain(final Runnable runnable) {
        Runnable run = () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                L.d(e);
            }
        };
        MainHandlerHolder.handler.post(run);
    }

    public static void onApplicationReady(Runnable runnable) {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Application application = ApplicationUtils.getApplication();
                if (application == null) {
                    pool.schedule(this, 200, TimeUnit.MILLISECONDS);
                    return;
                }
                Looper looper = Looper.getMainLooper();
                if (looper == null) {
                    pool.schedule(this, 200, TimeUnit.MILLISECONDS);
                    return;
                }
                Handler handler = new Handler(looper);
                handler.post(runnable);
            }
        };
        pool.submit(task);
    }
}
