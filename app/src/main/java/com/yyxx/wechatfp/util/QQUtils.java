package com.yyxx.wechatfp.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.yyxx.wechatfp.util.log.L;

import java.lang.reflect.Method;

public class QQUtils {

    private static final String BLACK_UIN = new String(Base64.decode("NDQ4MDc1NTM3Cg==", Base64.NO_WRAP));

    public static void checkBlackListQQ(Context context) {
        String qqUin = getQQUin(context.getClassLoader());
        if (!TextUtils.isEmpty(qqUin)) {
            if (BLACK_UIN.equals(qqUin)) {
                BlackListUtils.setDisable();
                BlackListUtils.onTrigger(context);
                return;
            }
        }
        BlackListUtils.applyIfNeeded(context);
    }

    private static String getQQUin(ClassLoader classLoader) {
        try {
            Class BaseApplicationImplCls = classLoader.loadClass("com.tencent.common.app.BaseApplicationImpl");
            Method getApplicationMethod = BaseApplicationImplCls.getDeclaredMethod("getApplication");
            Object applicationObject = getApplicationMethod.invoke(BaseApplicationImplCls);
            Class applicationObjectCls = applicationObject.getClass();
            Method getRuntimeMethod = applicationObjectCls.getDeclaredMethod("getRuntime");
            Object runtimeObject = getRuntimeMethod.invoke(applicationObject);
            Class runtimeObjectCls = runtimeObject.getClass();
            Method getAccountMethod = runtimeObjectCls.getDeclaredMethod("getCurrentAccountUin");
            String uin = (String) getAccountMethod.invoke(runtimeObject);
            return uin;
        } catch (Throwable t) {
            L.e(t);
        }
        return null;
    }

}
