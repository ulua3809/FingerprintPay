package com.surcumference.fingerprint.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityViewObserverHolder {
    public enum Key {
        AlipaySettingPageEntered,
        AlipayPasswordView,
        TaobaoPasswordView,
        WeChatPayView,
    }

    private static Map<Key, ActivityViewObserver> sHolder = new ConcurrentHashMap<>();

    public static void stop(Key key) {
        synchronized (ActivityViewObserverHolder.class) {
            ActivityViewObserver obs = sHolder.get(key);
            if (obs == null) {
                return;
            }
            obs.stop();
            sHolder.remove(key);
        }
    }

    public static void stop(ActivityViewObserver observer) {
        synchronized (ActivityViewObserverHolder.class) {
            observer.stop();
            Iterator<Map.Entry<Key, ActivityViewObserver>> it = sHolder.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Key, ActivityViewObserver> entry = it.next();
                if (entry.getValue() == observer) {
                    it.remove();
                }
            }
        }
    }

    public static void start(Key key, ActivityViewObserver activityViewObserver,
                             long loopMS, ActivityViewObserver.IActivityViewListener listener) {
        start(key, activityViewObserver, loopMS, listener, 0);
    }

    public static void start(Key key, ActivityViewObserver activityViewObserver,
                             long loopMS, ActivityViewObserver.IActivityViewListener listener,
                             long timeoutMS) {
        stop(key);
        synchronized (ActivityViewObserverHolder.class) {
            activityViewObserver.start(loopMS, listener);
            sHolder.put(key, activityViewObserver);
            if (timeoutMS > 0) {
                Task.onBackground(timeoutMS, () -> {
                    // 不会死锁
                    stop(key);
                });
            }
        }
    }

}
