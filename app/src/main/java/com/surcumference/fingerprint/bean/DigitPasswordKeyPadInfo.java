package com.surcumference.fingerprint.bean;

import java.util.HashMap;
import java.util.Map;

public class DigitPasswordKeyPadInfo {
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

        public Map<String, String[]> keys = new HashMap<>();

        public DigitPasswordKeyPadInfo(String modulePackageName, String[] key1, String[] key2, String[] key3, String[] key4, String[] key5, String[] key6, String[] key7, String[] key8, String[] key9, String[] key0) {
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
            this.keys.put("1", key1);
            this.keys.put("2", key2);
            this.keys.put("3", key3);
            this.keys.put("4", key4);
            this.keys.put("5", key5);
            this.keys.put("6", key6);
            this.keys.put("7", key7);
            this.keys.put("8", key8);
            this.keys.put("9", key9);
            this.keys.put("0", key0);
        }
    }
