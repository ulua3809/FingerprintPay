package com.yyxx.wechatfp.xposed.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.yyxx.wechatfp.BuildConfig;
import com.yyxx.wechatfp.util.log.L;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jason on 2017/9/8.
 */

public class XposedPluginLoader {

    //TODO 受Xposed機制影響 這玩意好像是廢的, 待檢查
    private static Map<Class, Object> sPluginCache = new HashMap<>();

    public static void load(Class pluginClz, Context context, XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        Object pluginObj;
        if (BuildConfig.DEBUG) {
            pluginObj = loadFromDex(context, pluginClz);
        } else {
            if ((pluginObj = sPluginCache.get(pluginClz)) == null) {
                synchronized (pluginClz) {
                    if ((pluginObj = sPluginCache.get(pluginClz)) == null) {
                        pluginObj = loadFromLocal(pluginClz);
                        sPluginCache.put(pluginClz, pluginObj);
                    }
                }
            }
        }
        callPluginMain(pluginObj, context, lpparam);
    }

    private static Object loadFromDex(Context context, Class pluginClz) throws Exception {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            String apkPath = info.sourceDir;
            //TODO multi process!
            String odexPath = context.getCacheDir().getAbsolutePath();
            //TODO has lib?
            String libPath = odexPath;
            ClassLoader xposedClassLoader = XposedPluginLoader.class.getClassLoader();
            ClassLoader apkClzLoader = new DexClassLoader(apkPath, odexPath, libPath, xposedClassLoader);
            forceClassLoaderReloadClasses(apkClzLoader, BuildConfig.APPLICATION_ID, apkPath);
            forceClassLoaderReloadClasses(apkClzLoader, "com.yyxx.wechatfp", apkPath);
            Method findClzMethod = BaseDexClassLoader.class.getDeclaredMethod("findClass", String.class);
            findClzMethod.setAccessible(true);
            Class<?> clz = (Class<?>) findClzMethod.invoke(apkClzLoader, pluginClz.getName());
            return clz.newInstance();
        } catch (PackageManager.NameNotFoundException ignore) {
        }
        return null;
    }

    private static Object loadFromLocal(Class pluginClz) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return pluginClz.newInstance();
    }

    private static void callPluginMain(Object pluginObj, Context context, XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Method method = pluginObj.getClass().getDeclaredMethod("main", Context.class, XC_LoadPackage.LoadPackageParam.class);
        method.invoke(pluginObj, context, lpparam);
    }

    private static boolean forceClassLoaderReloadClasses(ClassLoader classLoader, String packageNameStartWith, String apkPath) {
        try {
            Method findClzMethod = BaseDexClassLoader.class.getDeclaredMethod("findClass", String.class);
            findClzMethod.setAccessible(true);
            packageNameStartWith = packageNameStartWith + ".";
            DexFile dexFile = new DexFile(apkPath);
            Enumeration<String> classNames = dexFile.entries();
            while (classNames.hasMoreElements()) {
                String className = classNames.nextElement();
                if (className.startsWith(packageNameStartWith)) {
                    try {
                        findClzMethod.invoke(classLoader, className);
                    } catch (Exception e) {
                        L.d(e);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }
}
