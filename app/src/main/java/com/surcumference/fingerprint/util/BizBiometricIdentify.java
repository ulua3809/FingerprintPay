package com.surcumference.fingerprint.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BizBiometricIdentify extends XBiometricIdentify<BizBiometricIdentify> {

    private final Context context;
    public BizBiometricIdentify(Context context) {
        super(AESUtils.INSTANCE, context);
        this.context = context;
        Config config = Config.from(context);
        withUseBiometricApi(config.isUseBiometricApi());
    }

    public BizBiometricIdentify decryptPasscode(String passcodeEncrypted, IdentifyListener identifyListener) {
        Config config = Config.from(context);
        withDecryptionMode(passcodeEncrypted, AESUtils.hex2byte(config.getPasswordIV()), config.getPasswordEncKey());
        startIdentify(identifyListener);
        return this;
    }

    public BizBiometricIdentify encryptPasscode(String passcode, IdentifyListener identifyListener) {
        Config config = Config.from(context);
        withEncryptionMode(passcode, config.getPasswordEncKey());
        startIdentify(new IdentifyListener(identifyListener) {
            @Override
            public void onEncryptionSuccess(BizBiometricIdentify identify, @NonNull String encryptedContent, @Nullable byte[] encryptedIV) {
                config.setPasswordEncrypted(encryptedContent);
                config.setPasswordIV(AESUtils.byte2hex(encryptedIV != null ? encryptedIV : ("-fallback-place-holder-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)));
                config.commit();
                super.onEncryptionSuccess(identify, encryptedContent, encryptedIV);
            }
        });
        return this;
    }

    @Override
    protected void onNotify(NotifyEnum notifyEnum, Object... args) {
        super.onNotify(notifyEnum, args);
        switch (notifyEnum) {
            case OnBiometricNotEnable:
                NotifyUtils.notifyBiometricIdentify(this.context, Lang.getString(R.id.toast_fingerprint_not_enable));
                break;
            case OnQQVersionTooLow:
                Toaster.showLong(Lang.getString(R.id.toast_need_qq_7_2_5));
                break;
            case OnDecryptionFailed:
                NotifyUtils.notifyBiometricIdentify(this.context, Lang.getString(R.id.toast_fingerprint_password_dec_failed));
                break;
            case OnBiometricRetryEnded:
                NotifyUtils.notifyBiometricIdentify(this.context, Lang.getString(R.id.toast_fingerprint_retry_ended) + " " + args[0]);
                break;
            case OnBiometricLocked:
                NotifyUtils.notifyBiometricIdentify(this.context, Lang.getString(R.id.toast_fingerprint_unlock_reboot));
                break;
            case OnBiometricNotMatch:
                NotifyUtils.notifyBiometricIdentify(this.context, Lang.getString(R.id.toast_fingerprint_not_match));
                break;
            default:
                NotifyUtils.notifyBiometricIdentify(this.context, "Unknown notifyEnum: " + notifyEnum);
                break;
        }
    }

    public static class IdentifyListener extends XBiometricIdentify.IdentifyListener<BizBiometricIdentify> {
        public IdentifyListener() {
            super(null);
        }

        public IdentifyListener(IdentifyListener parentIdentifyListener) {
            super(parentIdentifyListener);
        }
    }
}
