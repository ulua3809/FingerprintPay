package com.surcumference.fingerprint.plugin.inf;

import javax.crypto.Cipher;

public interface OnFingerprintVerificationOKListener {
    void onFingerprintVerificationOK(Cipher cipher);
}
