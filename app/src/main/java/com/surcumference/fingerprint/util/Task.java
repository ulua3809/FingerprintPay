package com.surcumference.fingerprint.util;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.surcumference.fingerprint.util.log.L;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jason on 2017/9/10.
 */

public class Task {

    private static class MainHandlerHolder {
        private static Handler handler = new Handler(Looper.getMainLooper());
    }

    private static class BackgroundThreadPoolHolder {
        private static ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 16);
    }

    public static void onMain(final Runnable runnable) {
        onMain(0, runnable);
    }

    public static void onMain(long msec, final Runnable runnable) {
        Runnable run = () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                L.e(e);
            }
        };
        if (msec > 0) {
            MainHandlerHolder.handler.postDelayed(run, msec);
        } else {
            MainHandlerHolder.handler.post(run);
        }
    }

    public static void onBackground(final Runnable runnable) {
        onBackground(0, runnable);
    }

    public static void onBackground(long msec, final Runnable runnable) {
        Runnable run = () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                L.e(e);
            }
        };
        if (msec > 0) {
            BackgroundThreadPoolHolder.pool.schedule(run, msec, TimeUnit.MILLISECONDS);
        } else {
            BackgroundThreadPoolHolder.pool.submit(run);
        }
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
