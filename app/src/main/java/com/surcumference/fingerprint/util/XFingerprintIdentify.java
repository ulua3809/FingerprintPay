package com.surcumference.fingerprint.util;

import android.content.Context;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.plugin.inf.IMockCurrentUser;
import com.surcumference.fingerprint.util.log.L;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import javax.crypto.Cipher;

public class XFingerprintIdentify {

    private final Context context;
    private final FingerprintIdentify fingerprintIdentify;
    private IMockCurrentUser mockCurrentUserCallback;
    public boolean fingerprintScanStateReady = false;

    public XFingerprintIdentify(Context context) {
        this.context = context;
        Config config = Config.from(context);


        fingerprintIdentify = new FingerprintIdentify(context);
        fingerprintIdentify.setMaxAvailableTimes(8);
        fingerprintIdentify.setCipherMode(Cipher.DECRYPT_MODE, AESUtils.hex2byte(config.getPasswordIV()));
        fingerprintIdentify.setCipherKeyFallback(config.getPasswordEncKey());
        fingerprintIdentify.setSupportAndroidL(true);
        fingerprintIdentify.setUseBiometricApi(config.isUseBiometricApi());
        fingerprintIdentify.setExceptionListener(exception -> L.e("XFingerprintIdentify", exception));
    }

    public XFingerprintIdentify withMockCurrentUserCallback(IMockCurrentUser mockCurrentUserCallback) {
        this.mockCurrentUserCallback = mockCurrentUserCallback;
        return this;
    }

    public XFingerprintIdentify withEncryptionMode() {
        fingerprintIdentify.setCipherMode(Cipher.ENCRYPT_MODE, null);
        return this;
    }

    public XFingerprintIdentify withUseBiometricApi(boolean on) {
        fingerprintIdentify.setUseBiometricApi(on);
        return this;
    }

    public XFingerprintIdentify startIdentify(IdentifyListener identifyListener) {
        callMockCurrentUserCallback(true);
        fingerprintIdentify.init();
        if (!fingerprintIdentify.isFingerprintEnable()) {
            fingerprintScanStateReady = false;
            callMockCurrentUserCallback(false);
            if (Constant.PACKAGE_NAME_QQ.equals(context.getPackageName())) {
                if (PermissionUtils.hasFingerprintPermission(context)) {
                    L.d("系统指纹功能未启用");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_enable));
                } else {
                    L.d("QQ 版本过低");
                    Toaster.showLong(Lang.getString(R.id.toast_need_qq_7_2_5));
                }
            } else {
                NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_enable));
            }
            return this;
        }
        identifyListener.onInited(XFingerprintIdentify.this);
        fingerprintScanStateReady = true;
        fingerprintIdentify.startIdentify(new BaseFingerprint.IdentifyListener() {
            @Override
            public void onSucceed(Cipher cipher) {
                try {
                    identifyListener.onSucceed(XFingerprintIdentify.this, cipher);
                } finally {
                    callMockCurrentUserCallback(false);
                }
            }

            @Override
            public void onNotMatch(int availableTimes) {
                L.d("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_match));
                callMockCurrentUserCallback(false);
                identifyListener.onNotMatch(XFingerprintIdentify.this, availableTimes);
            }

            @Override
            public void onFailed(FingerprintIdentifyFailInfo failInfo) {
                try {
                    if (failInfo.throwable instanceof java.security.InvalidAlgorithmParameterException) {
                        NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_password_dec_failed));
                        identifyListener.onFailed(XFingerprintIdentify.this, failInfo);
                        return;
                    }
                    if (failInfo.isCancel()) {
                        identifyListener.onFailed(XFingerprintIdentify.this, failInfo);
                        return;
                    }
                    if (fingerprintScanStateReady) {
                        NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_retry_ended));
                    }
                    L.d("多次尝试错误，请确认指纹", failInfo);
                    identifyListener.onFailed(XFingerprintIdentify.this, failInfo);
                } finally {
                    callMockCurrentUserCallback(false);
                }
            }

            @Override
            public void onStartFailedByDeviceLocked() {
                try {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_unlock_reboot));
                    identifyListener.onFailed(XFingerprintIdentify.this, new FingerprintIdentifyFailInfo(true));
                } finally {
                    callMockCurrentUserCallback(false);
                }
            }
        });
        return this;
    }

    public void cancelIdentify() {
        fingerprintScanStateReady = false;
        fingerprintIdentify.cancelIdentify();
        callMockCurrentUserCallback(false);
    }

    public void resumeIdentify() {
        callMockCurrentUserCallback(true);
        fingerprintIdentify.resumeIdentify();
        fingerprintScanStateReady = fingerprintIdentify.isFingerprintEnable();
    }

    public boolean isUsingBiometricApi() {
        return fingerprintIdentify.isUsingBiometricApi();
    }

    private void callMockCurrentUserCallback(boolean mock) {
        IMockCurrentUser mockCurrentUserCallback = this.mockCurrentUserCallback;
        if (mockCurrentUserCallback == null) {
            return;
        }
        mockCurrentUserCallback.setMockCurrentUser(mock);
    }

    public static class IdentifyListener {

        public void onInited(XFingerprintIdentify identify) {

        }

        public void onSucceed(XFingerprintIdentify identify, Cipher cipher) {

        }

        public void onNotMatch(XFingerprintIdentify identify, int availableTimes) {

        }

        public void onFailed(XFingerprintIdentify identify, FingerprintIdentifyFailInfo failInfo) {

        }

        public void onStartFailedByDeviceLocked(XFingerprintIdentify identify) {

        }
    }

}
