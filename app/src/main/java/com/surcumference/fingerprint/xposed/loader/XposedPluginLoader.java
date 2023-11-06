package com.surcumference.fingerprint.xposed.loader;

import android.content.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedPluginLoader {

    private static Map<Class, Object> sPluginCache = new HashMap<>();

    public static void load(Class pluginClz, Context context, XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        Object pluginObj;
        if ((pluginObj = sPluginCache.get(pluginClz)) == null) {
            synchronized (pluginClz) {
                if ((pluginObj = sPluginCache.get(pluginClz)) == null) {
                    pluginObj = loadFromLocal(pluginClz);
                    sPluginCache.put(pluginClz, pluginObj);
                }
            }
        }
        callPluginMain(pluginObj, context, lpparam);
    }

    private static Object loadFromLocal(Class pluginClz) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return pluginClz.newInstance();
    }

    private static void callPluginMain(Object pluginObj, Context context, XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Method method = pluginObj.getClass().getDeclaredMethod("main", Context.class, XC_LoadPackage.LoadPackageParam.class);
        method.invoke(pluginObj, context, lpparam);
    }

}
