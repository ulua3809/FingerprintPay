package com.surcumference.fingerprint.util;

import android.text.TextUtils;

import com.surcumference.fingerprint.util.log.L;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;

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
}
