package com.surcumference.fingerprint.util;

import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.bean.DigitPasswordKeyPadInfo;

public class WeChatVersionControl {

    public static DigitPasswordKeyPadInfo getDigitPasswordKeyPad(int versionCode) {
        return new DigitPasswordKeyPadInfo(Constant.PACKAGE_NAME_WECHAT,
                new String[]{"tenpay_keyboard_1"},
                new String[]{"tenpay_keyboard_2"},
                new String[]{"tenpay_keyboard_3"},
                new String[]{"tenpay_keyboard_4"},
                new String[]{"tenpay_keyboard_5"},
                new String[]{"tenpay_keyboard_6"},
                new String[]{"tenpay_keyboard_7"},
                new String[]{"tenpay_keyboard_8"},
                new String[]{"tenpay_keyboard_9"},
                new String[]{"tenpay_keyboard_0"});
    }

}
