package com.surcumference.fingerprint.util;

import com.surcumference.fingerprint.util.log.L;

import java.lang.ref.WeakReference;

/**
 * 用这个类来阻止重复调用FingerprintIdentify.startIdentify, 防止业务调用startIdentify前忘记取消之前的调用
 * 可能出现的问题如下:
 * 1. Biometric API 验证界面出现又消失了
 * 2. javax.crypto.IllegalBlockSizeException
 *    Caused by:
 *        0: In update: Trying to get auth tokens.
 *        1: In AuthInfo::get_auth_tokens.
 *        2: In get_auth_tokens: No operation auth token received.
 *        3: Error::Km(ErrorCode(-26))) (public error code: 2 internal Keystore code: -26)
 *    ....
 */
public class XBiometricIdentifyManager {

    public static final XBiometricIdentifyManager INSTANCE = new XBiometricIdentifyManager();

    private WeakReference<XBiometricIdentify> lastXBiometricIdentifyRef;

    public void cancelFingerprintIdentify() {
        synchronized (XBiometricIdentifyManager.class) {
            try {
                WeakReference<XBiometricIdentify> lastXBiometricIdentifyRef = this.lastXBiometricIdentifyRef;
                if (lastXBiometricIdentifyRef == null) {
                    return;
                }
                XBiometricIdentify fingerprintIdentify = lastXBiometricIdentifyRef.get();
                if (fingerprintIdentify == null) {
                    return;
                }
                if (!fingerprintIdentify.fingerprintScanStateReady) {
                    return;
                }
                L.d("cancel last fingerprintIdentify!");
                fingerprintIdentify.cancelIdentify();
            } finally {
                this.lastXBiometricIdentifyRef = null;
            }
        }
    }
    public void set(XBiometricIdentify biometricIdentify) {
        synchronized (XBiometricIdentifyManager.class) {
            this.lastXBiometricIdentifyRef = biometricIdentify == null ? null : new WeakReference<>(biometricIdentify);
        }
    }
}
