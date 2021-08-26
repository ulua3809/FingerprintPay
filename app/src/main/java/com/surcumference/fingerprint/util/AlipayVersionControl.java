package com.surcumference.fingerprint.util;

public class AlipayVersionControl {

    public static DigitPasswordKeyPad getDigitPasswordKeyPad(int alipayVersion) {
        //10.2.0.8026
        if (alipayVersion >= 291) {
            return new DigitPasswordKeyPad("com.alipay.mobile.antui",
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
            return new DigitPasswordKeyPad("com.alipay.android.phone.safepaybase",
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

    public static class DigitPasswordKeyPad {
        public String modulePackageName;
        public String[] key1;
        public String[] key2;
        public String[] key3;
        public String[] key4;
        public String[] key5;
        public String[] key6;
        public String[] key7;
        public String[] key8;
        public String[] key9;
        public String[] key0;

        public DigitPasswordKeyPad(String modulePackageName, String[] key1, String[] key2, String[] key3, String[] key4, String[] key5, String[] key6, String[] key7, String[] key8, String[] key9, String[] key0) {
            this.modulePackageName = modulePackageName;
            this.key1 = key1;
            this.key2 = key2;
            this.key3 = key3;
            this.key4 = key4;
            this.key5 = key5;
            this.key6 = key6;
            this.key7 = key7;
            this.key8 = key8;
            this.key9 = key9;
            this.key0 = key0;
        }
    }
}
