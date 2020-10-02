package com.yyxx.wechatfp.util;

import com.yyxx.wechatfp.util.log.L;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;

public class FileUtils {

    public static void closeCloseable(Object cloeable) {
        try {
            if (cloeable == null) return;
            if (cloeable instanceof Closeable) {
                ((Closeable) cloeable).close();
            }
        } catch (Exception ignored) {
        }
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
