package com.surcumference.fingerprint.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.plugin.inf.IMockCurrentUser;
import com.surcumference.fingerprint.util.log.L;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;
import com.wei.android.lib.fingerprintidentify.util.PasswordCipherHelper;

import javax.crypto.Cipher;

public class XBiometricIdentify<T extends XBiometricIdentify>{

    private final Context context;
    private final FingerprintIdentify fingerprintIdentify;
    private final ICryptoHandler cryptoHandler;

    private IMockCurrentUser mockCurrentUserCallback;
    public boolean fingerprintScanStateReady = false;

    private String cipherFallbackKey;
    private String cipherContent;

    public XBiometricIdentify(ICryptoHandler cryptoHandler, Context context) {
        this.cryptoHandler = cryptoHandler;
        this.context = context;

        fingerprintIdentify = new FingerprintIdentify(context);
        fingerprintIdentify.setMaxAvailableTimes(8);
        fingerprintIdentify.setSupportAndroidL(true);
        fingerprintIdentify.setUseBiometricApi(false);
        fingerprintIdentify.setExceptionListener(exception -> L.e("XBiometricIdentify", exception));
    }

    public T withMockCurrentUserCallback(IMockCurrentUser mockCurrentUserCallback) {
        this.mockCurrentUserCallback = mockCurrentUserCallback;
        return (T)this;
    }

    public T withEncryptionMode(String cipherContent, String cipherFallbackKey) {
        this.cipherContent = cipherContent;
        this.cipherFallbackKey = cipherFallbackKey;
        fingerprintIdentify.setCipherMode(Cipher.ENCRYPT_MODE, null);
        return (T)this;
    }

    public T withDecryptionMode(String cipherContent, byte[] cipherIV, String cipherFallbackKey) {
        this.cipherContent = cipherContent;
        this.cipherFallbackKey = cipherFallbackKey;
        fingerprintIdentify.setCipherMode(Cipher.DECRYPT_MODE, cipherIV);
        return (T)this;
    }

    public T withUseBiometricApi(boolean on) {
        fingerprintIdentify.setUseBiometricApi(on);
        return (T)this;
    }

    public T startIdentify(IdentifyListener identifyListener) {
        XBiometricIdentifyManager xBiometricIdentifyManager =  XBiometricIdentifyManager.INSTANCE;
        xBiometricIdentifyManager.cancelFingerprintIdentify();
        xBiometricIdentifyManager.set(this);
        int cipherMode = fingerprintIdentify.getCipherMode();
        if (cipherMode == Cipher.ENCRYPT_MODE) {
        } else if (cipherMode == Cipher.DECRYPT_MODE) {
        } else {
            throw new RuntimeException("Encrypt mode or decrypt mode not call");
        }
        callMockCurrentUserCallback(true);
        fingerprintIdentify.init();
        if (!fingerprintIdentify.isFingerprintEnable()) {
            fingerprintScanStateReady = false;
            callMockCurrentUserCallback(false);
            if (Constant.PACKAGE_NAME_QQ.equals(context.getPackageName())) {
                if (PermissionUtils.hasFingerprintPermission(context)) {
                    L.d("系统指纹功能未启用");
                    onNotify(NotifyEnum.OnBiometricNotEnable);
                } else {
                    L.d("QQ 版本过低");
                    onNotify(NotifyEnum.OnQQVersionTooLow);
                }
            } else {
                onNotify(NotifyEnum.OnBiometricNotEnable);
            }
            return (T)this;
        }
        identifyListener.onInited(XBiometricIdentify.this);
        fingerprintScanStateReady = true;
        fingerprintIdentify.startIdentify(new BaseFingerprint.IdentifyListener() {
            @Override
            public void onSucceed(@Nullable Cipher cipher) {
                try {
                    xBiometricIdentifyManager.set(null);
                    String encryptedOrDecryptedContent = null;
                    for (int i = 0; i < 2; i++) {
                        if (cipher != null) {
                            encryptedOrDecryptedContent = encryptionOrDecryption(cipherMode, cipher, cipherContent);
                            if (encryptedOrDecryptedContent != null) {
                                if (cipherMode == Cipher.ENCRYPT_MODE) {
                                    identifyListener.onEncryptionSuccess(XBiometricIdentify.this, encryptedOrDecryptedContent, cipher.getIV());
                                } else if (cipherMode == Cipher.DECRYPT_MODE) {
                                    identifyListener.onDecryptionSuccess(XBiometricIdentify.this, encryptedOrDecryptedContent);
                                }
                                return;
                            }
                        }
                        cipher = PasswordCipherHelper.createCipher(cipherMode, cipherFallbackKey);
                    }
                    
                    if (cipherMode == Cipher.DECRYPT_MODE) {
                        Toaster.showShort(Lang.getString(R.id.toast_fingerprint_password_dec_failed));
                        return;
                    }

                    throw new RuntimeException("Unable encryptionOrDecryption text: " + cipherContent);
                } finally {
                    callMockCurrentUserCallback(false);
                }
            }

            @Override
            public void onNotMatch(int availableTimes) {
                L.d("指纹识别失败，还可尝试" + String.valueOf(availableTimes) + "次");
                onNotify(NotifyEnum.OnBiometricNotMatch);
                callMockCurrentUserCallback(false);
                identifyListener.onNotMatch(XBiometricIdentify.this, availableTimes);
            }

            @Override
            public void onFailed(FingerprintIdentifyFailInfo failInfo) {
                try {
                    xBiometricIdentifyManager.set(null);
                    if (failInfo.throwable instanceof java.security.InvalidAlgorithmParameterException) {
                        onNotify(NotifyEnum.OnDecryptionFailed);
                        identifyListener.onFailed(XBiometricIdentify.this, failInfo);
                        return;
                    }
                    if (failInfo.isCancel()) {
                        identifyListener.onFailed(XBiometricIdentify.this, failInfo);
                        return;
                    }
                    if (fingerprintScanStateReady) {
                        onNotify(NotifyEnum.OnBiometricRetryEnded, failInfo);
                    }
                    L.d("多次尝试错误，请确认指纹", failInfo);
                    identifyListener.onFailed(XBiometricIdentify.this, failInfo);
                } finally {
                    callMockCurrentUserCallback(false);
                }
            }

            @Override
            public void onStartFailedByDeviceLocked() {
                try {
                    xBiometricIdentifyManager.set(null);
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    L.d("系统限制，重启后必须验证密码后才能使用指纹验证");
                    onNotify(NotifyEnum.OnBiometricLocked);
                    identifyListener.onFailed(XBiometricIdentify.this, new FingerprintIdentifyFailInfo(true));
                } finally {
                    callMockCurrentUserCallback(false);
                }
            }
        });
        return (T)this;
    }

    private String encryptionOrDecryption(int cipherMode, Cipher cipher, String cipherContent) {
        try {
            if (cipher != null) {
                if (cipherMode == Cipher.ENCRYPT_MODE) {
                    return cryptoHandler.encrypt(cipher, cipherContent);
                } else if (cipherMode == Cipher.DECRYPT_MODE) {
                    return cryptoHandler.decrypt(cipher, cipherContent);
                } else {
                    throw new RuntimeException("Unsupported cipher mode: " + cipherMode);
                }
            }
        } catch (Exception e) {
            L.e(e);
        }
        return null;
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

    protected void onNotify(NotifyEnum notifyEnum, Object...args) {

    }

    public static class IdentifyListener<T extends XBiometricIdentify> {

        private IdentifyListener<T> parentIdentifyListener;

        public IdentifyListener() {
            this(null);
        }

        public IdentifyListener(IdentifyListener<T> parentIdentifyListener) {
            this.parentIdentifyListener = parentIdentifyListener;
        }

        public void onInited(T identify) {
            IdentifyListener<T> listener = this.parentIdentifyListener;
            if (listener != null) {
                listener.onInited(identify);
            }
        }

        public void onDecryptionSuccess(T identify, @NonNull String decryptedContent) {
            IdentifyListener<T> listener = this.parentIdentifyListener;
            if (listener != null) {
                listener.onDecryptionSuccess(identify, decryptedContent);
            }
        }
        
        public void onEncryptionSuccess(T identify, @NonNull String encryptedContent, @Nullable byte[] encryptedIV) {
            IdentifyListener<T> listener = this.parentIdentifyListener;
            if (listener != null) {
                listener.onEncryptionSuccess(identify, encryptedContent, encryptedIV);
            }
        }

        public void onNotMatch(T identify, int availableTimes) {
            IdentifyListener<T> listener = this.parentIdentifyListener;
            if (listener != null) {
                listener.onNotMatch(identify, availableTimes);
            }
        }

        public void onFailed(T identify, FingerprintIdentifyFailInfo failInfo) {
            IdentifyListener<T> listener = this.parentIdentifyListener;
            if (listener != null) {
                listener.onFailed(identify, failInfo);
            }
        }

        public void onStartFailedByDeviceLocked(T identify) {
            IdentifyListener<T> listener = this.parentIdentifyListener;
            if (listener != null) {
                listener.onStartFailedByDeviceLocked(identify);
            }
        }
    }

    public interface ICryptoHandler {
        @Nullable
        String decrypt(@NonNull Cipher cipher, @NonNull String content);
        @Nullable
        String encrypt(@NonNull Cipher cipher, @NonNull String content);
    }

    public enum NotifyEnum {
        OnBiometricNotEnable,
        OnQQVersionTooLow,
        OnDecryptionFailed,
        OnBiometricRetryEnded,
        OnBiometricLocked,
        OnBiometricNotMatch,
    }

}
