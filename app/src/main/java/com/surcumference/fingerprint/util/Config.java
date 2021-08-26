package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.util.log.L;

import java.util.WeakHashMap;

/**
 * Created by Jason on 2017/9/9.
 */

public class Config {


    private static WeakHashMap<Context, ObjectCache> sConfigCache = new WeakHashMap<>();

    public static Config from(Context context) {
        return new Config(context);
    }

    private ObjectCache mCache;

    private Config(Context context) {
        if (sConfigCache.containsKey(context)) {
            mCache = sConfigCache.get(context);
        }
        if (mCache == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".settings", Context.MODE_PRIVATE);
            String deviceId = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
            int passwordEncKey = deviceId.hashCode();
            SharedPreferences mainAppSharePreference;
            try {
                mainAppSharePreference = XPreferenceProvider.getRemoteSharedPreference(context);
            } catch (Exception e) {
                mainAppSharePreference = sharedPreferences;
                L.e(e);
            }
            mCache = new ObjectCache(sharedPreferences, mainAppSharePreference, passwordEncKey);
            sConfigCache.put(context, mCache);
        }
    }

    public boolean isOn() {
        return mCache.sharedPreferences.getBoolean("switch_on1", false);
    }

    public void setOn(boolean on) {
        mCache.sharedPreferences.edit().putBoolean("switch_on1", on).apply();
    }

    @Nullable
    public String getPassword() {
        String enc = mCache.sharedPreferences.getString("password", null);
        if (TextUtils.isEmpty(enc)) {
            return null;
        }
        return AESUtils.decrypt(enc, String.valueOf(mCache.passwordEncKey));
    }

    public void setPassword(String password) {
        String enc = AESUtils.encrypt(password, String.valueOf(mCache.passwordEncKey));
        mCache.sharedPreferences.edit().putString("password", enc).apply();
    }

    public void setSkipVersion(String version) {
        mCache.sharedPreferences.edit().putString("skip_version", version).apply();
        mCache.mainAppSharedPreferences.edit().putString("skip_version", version).apply();
    }

    @Nullable
    public String getSkipVersion() {
        String skipVersion = mCache.mainAppSharedPreferences.getString("skip_version", null);
        if (TextUtils.isEmpty(skipVersion)) {
            skipVersion = mCache.sharedPreferences.getString("skip_version", null);
        }
        return skipVersion;
    }

    public void setLicenseAgree(boolean agree) {
        mCache.sharedPreferences.edit().putBoolean("license_agree", agree).apply();
        mCache.mainAppSharedPreferences.edit().putBoolean("license_agree", agree).apply();
    }

    public boolean getLicenseAgree() {
        boolean agree = mCache.mainAppSharedPreferences.getBoolean("license_agree", false);
        if (!agree) {
            agree = mCache.sharedPreferences.getBoolean("license_agree", false);
        }
        return agree;
    }

    private class ObjectCache {
        SharedPreferences sharedPreferences;
        SharedPreferences mainAppSharedPreferences;
        int passwordEncKey;

        public ObjectCache(SharedPreferences sharedPreferences, SharedPreferences mainAppSharedPreferences,int passwordEncKey) {
            this.sharedPreferences = sharedPreferences;
            this.mainAppSharedPreferences = mainAppSharedPreferences;
            this.passwordEncKey = passwordEncKey;
        }
    }
}
