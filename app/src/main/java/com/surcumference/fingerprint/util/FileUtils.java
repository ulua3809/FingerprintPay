package com.surcumference.fingerprint.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.util.log.L;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.concurrent.Callable;

public class FileUtils {
    /**
     * 复制文件
     *
     * @param s 源文件
     * @param t 复制到的新文件
     */

    public static boolean copy(File s, File t) {
        FileInputStream fi = null;
        FileOutputStream fo = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            fi = new FileInputStream(s);
            bis = new BufferedInputStream(fi);
            fo = new FileOutputStream(t);
            bos = new BufferedOutputStream(fo);

            byte[] buf = new byte[1024];
            int len;
            while ((len = bis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
        } catch (Exception e) {
            L.e(e);
            return false;
        } finally {
            closeCloseable(fi);
            closeCloseable(bis);
            closeCloseable(bos);
            closeCloseable(fo);
        }
        return true;
    }

    public static void closeCloseable(Object cloeable) {
        try {
            if (cloeable == null) return;
            if (cloeable instanceof Closeable) {
                ((Closeable) cloeable).close();
            }
        } catch (Exception ignored) {
        }
    }

    public static String getCmdLineContentByPid(int pid) {
        if (pid == 0) {
            return null;
        }
        String path = "/proc/" + pid + "/cmdline";
        File file = new File(path);
        if (file.exists()) {
            String statusContent;
            FileInputStream fis = null;
            BufferedInputStream ir = null;
            StringReader rd = null;
            try {
                byte statusBytes[] = new byte[512];
                fis = new FileInputStream(file);
                ir = new BufferedInputStream(fis, 32);
                ir.read(statusBytes);
                statusContent = new String(statusBytes);
                if (!TextUtils.isEmpty(statusContent)) {
                    statusContent = statusContent.trim();
                }
                return statusContent;
            } catch (Exception | Error e) {
            } finally {
                closeCloseable(rd);
                closeCloseable(ir);
                closeCloseable(fis);
            }
        }
        return null;
    }

    public static void write(File file, String content) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
        } catch (Throwable t) {
            L.e(t);
        } finally {
            closeCloseable(fos);
        }
    }

    public static Uri getUri(Context context, File file) {
        String authority = getAuthorityForPackage(context.getPackageName());
        return FileProvider.getUriForFile(context, authority, file);
    }

    public static String getAuthorityForPackage(String packageName) {
        if (Constant.PACKAGE_NAME_ALIPAY.equals(packageName)) {
            return Constant.AUTHORITY_ALIPAY;
        } else if (Constant.PACKAGE_NAME_QQ.equals(packageName)) {
            return Constant.AUTHORITY_QQ;
        } else if (Constant.PACKAGE_NAME_TAOBAO.equals(packageName)) {
            return Constant.AUTHORITY_TAOBAO;
        } else if (Constant.PACKAGE_NAME_WECHAT.equals(packageName)) {
            return Constant.AUTHORITY_WECHAT;
        } else if (Constant.PACKAGE_NAME_UNIONPAY.equals(packageName)) {
            return Constant.AUTHORITY_UNIONPAY;
        } else if (BuildConfig.APPLICATION_ID.equals(packageName)) {
            return Constant.AUTHORITY_FINGERPRINT_PAY;
        } else {
            throw new RuntimeException("getAuthorityForPackage package:" + packageName + " is not support yet");
        }
    }

    public static File getSharableFile(Context context, String fileName) {
        Callable<File>[] testPathCallArrays = new Callable[] {
                () -> new File(context.getCacheDir(), fileName), //taobao
                () -> new File(context.getFilesDir(), fileName),
                () -> new File(context.getDir("storage", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Context.MODE_PRIVATE : Context.MODE_WORLD_WRITEABLE), fileName), //alipay
                () -> new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName), //UnionPay
        };

        for (int i = 0; i < testPathCallArrays.length; i++) {
            try {
                File testFile = testPathCallArrays[i].call();
                getUri(context, testFile);
                return testFile;
            } catch (Exception e) {
            }
        }
        throw new RuntimeException("getSharableFile need more rule");
    }


    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param file 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful. If a
     * deletion fails, the method stops attempting to delete and returns
     * "false".
     */
    public static boolean delete(File file) {
        if (file == null || !file.exists())
            return true;

        if (file.isDirectory()) {
            String[] children = file.list();
            // 递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = delete(new File(file, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return file.delete();
    }
}
