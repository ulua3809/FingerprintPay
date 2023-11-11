package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;

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
    public String getPasswordEncrypted() {
        String pwd = mCache.sharedPreferences.getString("password_2", null);
        if (TextUtils.isEmpty(pwd)) {
            return null;
        }
        return pwd;
    }

    public void setPasswordEncrypted(String password) {
        mCache.sharedPreferences.edit().putString("password_2", password).apply();
    }

    @Nullable
    public String getPasswordIV() {
        return mCache.sharedPreferences.getString("password_iv", null);
    }

    public void setPasswordIV(String iv) {
        mCache.sharedPreferences.edit().putString("password_iv", iv).apply();
    }

    public String getPasswordEncKey() {
        return String.valueOf(mCache.passwordEncKey);
    }

    public boolean isShowFingerprintIcon() {
        return mCache.sharedPreferences.getBoolean("fingerprint_icon", true);
    }

    public void setShowFingerprintIcon(boolean on) {
        mCache.sharedPreferences.edit().putBoolean("fingerprint_icon", on).apply();
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

    public void commit() {
        mCache.sharedPreferences.edit().commit();
        mCache.mainAppSharedPreferences.edit().commit();
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
