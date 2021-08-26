package com.surcumference.fingerprint.xposed.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.util.ReflectionUtils;
import com.surcumference.fingerprint.util.log.L;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.BaseDexClassLoader;
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
        File apkFile = getModuleApkFile(context);
        File odexDir = context.getCacheDir();
        ClassLoader xposedClassLoader = XposedPluginLoader.class.getClassLoader();
        hijackDexElements(xposedClassLoader, apkFile, odexDir);
        Method findClzMethod = BaseDexClassLoader.class.getDeclaredMethod("findClass", String.class);
        findClzMethod.setAccessible(true);
        Class<?> clz = (Class<?>) findClzMethod.invoke(xposedClassLoader, pluginClz.getName());
        return clz.newInstance();
    }

    private static File getModuleApkFile(Context context) {
        try {
            ApplicationInfo info = context.getApplicationContext().getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            return new File(info.sourceDir);
        } catch (PackageManager.NameNotFoundException e) {
            L.e(e);
        }
        return new File("/data/local/tmp/" + BuildConfig.APPLICATION_ID + ".apk");
    }

    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_VALUE = "value";

    private static Map<String, Object> queryAll(Uri baseUri, Context context) {
        Uri uri = baseUri.buildUpon().appendPath("").build();
        String[] columns = {COLUMN_KEY, COLUMN_TYPE, COLUMN_VALUE};
        Cursor cursor = query(context, uri, columns);
        try {
            HashMap<String, Object> map = new HashMap<String, Object>();
            if (cursor == null) {
                return map;
            }
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                map.put(name, getValue(cursor, 1, 2));
            }
            return map;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static Object getValue(Cursor cursor, int typeCol, int valueCol) {
        int expectedType = cursor.getInt(typeCol);
        switch (expectedType) {
            case RemoteContract.TYPE_STRING:
                return cursor.getString(valueCol);
            case RemoteContract.TYPE_STRING_SET:
                return cursor.getString(valueCol);
            case RemoteContract.TYPE_INT:
                return cursor.getInt(valueCol);
            case RemoteContract.TYPE_LONG:
                return cursor.getLong(valueCol);
            case RemoteContract.TYPE_FLOAT:
                return cursor.getFloat(valueCol);
            case RemoteContract.TYPE_BOOLEAN:
                return cursor.getInt(valueCol) != 0;
            default:
                throw new AssertionError("Invalid expected type: " + expectedType);
        }
    }
    public static class  RemoteContract {
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_VALUE = "value";
        public static final String[] COLUMN_ALL = {
                RemoteContract.COLUMN_KEY,
                RemoteContract.COLUMN_TYPE,
                RemoteContract.COLUMN_VALUE
        };

        public static final int TYPE_NULL = 0;
        public static final int TYPE_STRING = 1;
        public static final int TYPE_STRING_SET = 2;
        public static final int TYPE_INT = 3;
        public static final int TYPE_LONG = 4;
        public static final int TYPE_FLOAT = 5;
        public static final int TYPE_BOOLEAN = 6;
    }



    private static Cursor query(Context context, Uri uri, String[] columns) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, columns, null, null, null);
        } catch (Exception e) {
            L.e(e);
        }
        return cursor;
    }
    private static void hijackDexElements(ClassLoader classLoader, File apkFile, File odexFile) {
        try {
            Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(classLoader);
            Class DexPathListClass = pathList.getClass();
            Field dexElementsField = DexPathListClass.getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object[] dexElements = (Object[])dexElementsField.get(pathList);
            for (Object dexElement : dexElements) {
                L.d("dexElement", dexElement);
            }
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            ArrayList<File> apkFileList = new ArrayList<>();
            apkFileList.add(apkFile);
            Object[] apkDexElements = makePathElements(pathList, apkFileList, odexFile, suppressedExceptions);
            if (suppressedExceptions.size() > 0) {
                for (IOException e : suppressedExceptions) {
                    L.e("Exception in makePathElements", e);
                }
            }
            dexElementsField.set(pathList, apkDexElements);
        } catch (Exception e){
            L.e(e);
        }
    }

    private static Object[] makePathElements(
            Object dexPathList, ArrayList<File> files, File optimizedDirectory,
            ArrayList<IOException> suppressedExceptions)
            throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        Method makeDexElements =
                ReflectionUtils.findMethod(dexPathList, "makePathElements", List.class, File.class,
                        List.class);
        return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                suppressedExceptions);
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
