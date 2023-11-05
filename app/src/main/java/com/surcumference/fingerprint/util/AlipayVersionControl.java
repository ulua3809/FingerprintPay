package com.surcumference.fingerprint.util;

import com.surcumference.fingerprint.bean.DigitPasswordKeyPadInfo;

public class AlipayVersionControl {

    public static DigitPasswordKeyPadInfo getDigitPasswordKeyPad(int alipayVersion) {
        //10.2.0.8026
        if (alipayVersion >= 291) {
            return new DigitPasswordKeyPadInfo("com.alipay.mobile.antui",
                    new String[]{"au_num_1"},
                    new String[]{"au_num_2"},
                    new String[]{"au_num_3"},
                    new String[]{"au_num_4"},
                    new String[]{"au_num_5"},
                    new String[]{"au_num_6"},
                    new String[]{"au_num_7"},
                    new String[]{"au_num_8"},
                    new String[]{"au_num_9"},
                    new String[]{"au_num_0"});
        } else {
            return new DigitPasswordKeyPadInfo("com.alipay.android.phone.safepaybase",
                    new String[]{"key_num_1"},
                    new String[]{"key_num_2"},
                    new String[]{"key_num_3"},
                    new String[]{"key_num_4", "key_4"},
                    new String[]{"key_num_5"},
                    new String[]{"key_num_6"},
                    new String[]{"key_num_7"},
                    new String[]{"key_num_8"},
                    new String[]{"key_num_9"},
                    new String[]{"key_num_0"});
        }
    }
}
