package com.surcumference.fingerprint.util;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.zip.ZipEngine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * ZIP压缩文件操作工具类
 * 支持密码
 * 依赖zip4j开源项目(http://www.lingala.net/zip4j/)
 * 版本1.3.1
 *
 * @author ninemax
 */
public class ZipUtils {

    public static File[] unzip(File zipFile, File destDir) throws ZipException {
        return unzip(zipFile, destDir.getAbsolutePath(), "");
    }

    /**
     * 使用给定密码解压指定的ZIP压缩文件到指定目录
     * <p/>
     * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出
     *
     * @param zip    指定的ZIP压缩文件
     * @param dest   解压目录
     * @param passwd ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static File[] unzip(String zip, String dest, String passwd) throws ZipException {
        File zipFile = new File(zip);
        return unzip(zipFile, dest, passwd);
    }

    /**
     * 使用给定密码解压指定的ZIP压缩文件到当前目录
     *
     * @param zip    指定的ZIP压缩文件
     * @param passwd ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static File[] unzip(String zip, String passwd) throws ZipException {
        File zipFile = new File(zip);
        File parentDir = zipFile.getParentFile();
        return unzip(zipFile, parentDir.getAbsolutePath(), passwd);
    }

    /**
     * 使用给定密码解压指定的ZIP压缩文件到指定目录
     * <p/>
     * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出
     *
     * @param zipFile 指定的ZIP压缩文件
     * @param dest    解压目录
     * @param passwd  ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static File[] unzip(File zipFile, String dest, String passwd) throws ZipException {
        return unzip(zipFile, dest, passwd, "GBK");
    }

    public static File[] unzip(File zipFile, String dest, String passwd, String charset) throws ZipException {
        ZipFile zFile = new ZipFile(zipFile);
        zFile.setFileNameCharset(charset);
        if (!zFile.isValidZipFile()) {
            throw new ZipException("压缩文件不合法,可能被损坏.");
        }
        File destDir = new File(dest);
        if (destDir.isDirectory() && !destDir.exists()) {
            destDir.mkdirs();
        }
        if (zFile.isEncrypted()) {
            zFile.setPassword(passwd.toCharArray());
        }
        zFile.extractAll(dest);

        List<FileHeader> headerList = zFile.getFileHeaders();
        List<File> extractedFileList = new ArrayList<File>();
        for (FileHeader fileHeader : headerList) {
            if (!fileHeader.isDirectory()) {
                extractedFileList.add(new File(destDir, fileHeader.getFileName()));
            }
        }
        File[] extractedFiles = new File[extractedFileList.size()];
        extractedFileList.toArray(extractedFiles);
        return extractedFiles;
    }


    /**
     * 使用给定密码解压指定的ZIP压缩文件的指定文件到指定目录
     * <p/>
     * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出
     *
     * @param zipFile 指定的ZIP压缩文件
     * @param target  要解压的文件
     * @param dest    解压目录
     * @param passwd  ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static void unzipTarget(File zipFile, String target, String dest, String passwd) throws ZipException {
        ZipFile zFile = new ZipFile(zipFile);
        File destFile = new File(dest);
        File destParentDir = destFile.getParentFile();
        if (destFile.exists()) {
            destFile.delete();
        }
        if (destParentDir.exists() && !destParentDir.isFile()) {
            destParentDir.delete();
        }
        if (destParentDir.isDirectory() && !destParentDir.exists()) {
            destParentDir.mkdirs();
        }
        if (zFile.isEncrypted()) {
            zFile.setPassword(passwd.toCharArray());
        }
        zFile.extractFile(target, destParentDir.getAbsolutePath(), null, destFile.getName());
    }


    public static long getCrc32(File zipFile, String target) throws ZipException {
        ZipFile zFile = new ZipFile(zipFile);
        FileHeader fileHeader = zFile.getFileHeader(target);
        if (fileHeader == null) {
            return 0;
        }
        return fileHeader.getCrc32();
    }


    public static void unzipTarget(File zipFile, String target, File destFile) throws ZipException {
        unzipTarget(zipFile, target, destFile.getAbsolutePath(), "");
    }

    /**
     * 解压指定的ZIP压缩文件的指定文件到指定目录
     * <p/>
     * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出
     *
     * @param zipFile 指定的ZIP压缩文件
     * @param target  要解压的文件
     * @param dest    解压目录
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static void unzipTarget(File zipFile, String target, String dest) throws ZipException {
        unzipTarget(zipFile, target, dest, "");
    }

    /**
     * 压缩指定文件到当前文件夹
     *
     * @param src 要压缩的指定文件
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src) {
        return zip(src, null);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到当前目录
     *
     * @param src    要压缩的文件
     * @param passwd 压缩使用的密码
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src, String passwd) {
        return zip(src, null, passwd);
    }

    public static String zip(File srcFile, File destFile) {
        return zip(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), true, "", "GBK");
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到当前目录
     *
     * @param src    要压缩的文件
     * @param dest   压缩文件存放路径
     * @param passwd 压缩使用的密码
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src, String dest, String passwd) {
        return zip(src, dest, true, passwd);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到指定位置.
     * <p/>
     * dest可传最终压缩文件存放的绝对路径,也可以传存放目录,也可以传null或者"".<br />
     * 如果传null或者""则将压缩文件存放在当前目录,即跟源文件同目录,压缩文件名取源文件名,以.zip为后缀;<br />
     * 如果以路径分隔符(File.separator)结尾,则视为目录,压缩文件名取源文件名,以.zip为后缀,否则视为文件名.
     *
     * @param src         要压缩的文件或文件夹路径
     * @param dest        压缩文件存放路径
     * @param isCreateDir 是否在压缩文件里创建目录,仅在压缩文件为目录时有效.<br />
     *                    如果为false,将直接压缩目录下文件到压缩文件.
     * @param passwd      压缩使用的密码
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src, String dest, boolean isCreateDir, String passwd) {
        return zip(src, dest, isCreateDir, passwd, "UTF-8");
    }

    public static String zip(String src, String dest, boolean isCreateDir, String passwd, String charset) {
        File srcFile = new File(src);
        dest = buildDestinationZipFilePath(srcFile, dest);
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);           // 压缩方式
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);    // 压缩级别
        if (!passwd.equals("")) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD); // 加密方式
            parameters.setPassword(passwd.toCharArray());
        }
        try {
            ZipFile zipFile = new ZipFile(dest);
            zipFile.setFileNameCharset(charset);
            if (srcFile.isDirectory()) {
                // 如果不创建目录的话,将直接把给定目录下的文件压缩到压缩文件,即没有目录结构
                if (!isCreateDir) {
                    File[] subFiles = srcFile.listFiles();
                    ArrayList<File> temp = new ArrayList<File>();
                    Collections.addAll(temp, subFiles);
                    zipFile.addFiles(temp, parameters);
                    return dest;
                }
                zipFile.addFolder(srcFile, parameters);
            } else {
                zipFile.addFile(srcFile, parameters);
            }
            return dest;
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 构建压缩文件存放路径,如果不存在将会创建
     * 传入的可能是文件名或者目录,也可能不传,此方法用以转换最终压缩文件的存放路径
     *
     * @param srcFile   源文件
     * @param destParam 压缩目标路径
     * @return 正确的压缩文件存放路径
     */
    private static String buildDestinationZipFilePath(File srcFile, String destParam) {
        if (destParam != null && destParam.equals("")) {
            if (srcFile.isDirectory()) {
                destParam = srcFile.getParent() + File.separator + srcFile.getName() + ".zip";
            } else {
                String fileName = srcFile.getName().substring(0, srcFile.getName().lastIndexOf("."));
                destParam = srcFile.getParent() + File.separator + fileName + ".zip";
            }
        } else {
            createDestDirectoryIfNecessary(destParam);  // 在指定路径不存在的情况下将其创建出来
            if (destParam.endsWith(File.separator)) {
                String fileName = "";
                if (srcFile.isDirectory()) {
                    fileName = srcFile.getName();
                } else {
                    fileName = srcFile.getName().substring(0, srcFile.getName().lastIndexOf("."));
                }
                destParam += fileName + ".zip";
            }
        }
        return destParam;
    }

    /**
     * 在必要的情况下创建压缩文件存放目录,比如指定的存放路径并没有被创建
     *
     * @param destParam 指定的存放路径,有可能该路径并没有被创建
     */
    private static void createDestDirectoryIfNecessary(String destParam) {
        File destDir = null;
        if (destParam.endsWith(File.separator)) {
            destDir = new File(destParam);
        } else {
            destDir = new File(destParam.substring(0, destParam.lastIndexOf(File.separator)));
        }
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
    }

    @WorkerThread
    public static void addFileToZip(ZipFile zipFile, File file, String fileName,
                                    ZipParameters parameters) throws ZipException, CloneNotSupportedException {
        parameters = (ZipParameters)parameters.clone();
        parameters.setFileNameInZip(fileName);
        parameters.setSourceExternalStream(true);
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            zipFile.addStream(bis, parameters);
        } catch (Exception e) {
            throw new ZipException(e);
        } finally {
            FileUtils.closeCloseable(bis);
            FileUtils.closeCloseable(fis);
        }
    }

    /**
     * @see ZipFile#addFolder(File, net.lingala.zip4j.model.ZipParameters)
     */
    public static void addFolder(ZipFile zipFile, File file, ZipParameters parameters) throws ZipException, CloneNotSupportedException {
        addFolder(zipFile, file, null, parameters, null);
    }

    /**
     * @see ZipFile#addFolder(File, net.lingala.zip4j.model.ZipParameters)
     */
    public static void addFolder(ZipFile zipFile, File file, ZipParameters parameters, @Nullable FileFilter fileFilter) throws ZipException, CloneNotSupportedException {
        addFolder(zipFile, file, null, parameters, fileFilter);
    }

    /**
     * @see ZipFile#addFolder(File, net.lingala.zip4j.model.ZipParameters)
     */
    public static void addFolder(ZipFile zipFile, File file, @Nullable String toDirInZip, ZipParameters parameters) throws ZipException, CloneNotSupportedException {
        addFolder(zipFile, file, toDirInZip, parameters, null);
    }

    /**
     * @see ZipFile#addFolder(File, net.lingala.zip4j.model.ZipParameters)
     */
    public static void addFolder(ZipFile zipFile, File file, @Nullable String toDirInZip, ZipParameters parameters, @Nullable FileFilter fileFilter) throws ZipException, CloneNotSupportedException {
        if (file == null) {
            throw new ZipException("input path is null, cannot add folder to zip file");
        }

        if (parameters == null) {
            throw new ZipException("input parameters are null, cannot add folder to zip file");
        }
        if (!TextUtils.isEmpty(toDirInZip)) {
            parameters = (ZipParameters) parameters.clone();
            parameters.setRootFolderInZip(toDirInZip);
        }
        addFolderToZip(zipFile, file, parameters, fileFilter);
    }

    /**
     * @see ZipEngine#addFolderToZip(File, net.lingala.zip4j.model.ZipParameters, net.lingala.zip4j.progress.ProgressMonitor, boolean)
     */
    public static void addFolderToZip(ZipFile zipFile, File file, ZipParameters parameters) throws ZipException {
        addFolderToZip(zipFile, file, parameters, null);
    }

    /**
     * @see ZipEngine#addFolderToZip(File, net.lingala.zip4j.model.ZipParameters, net.lingala.zip4j.progress.ProgressMonitor, boolean)
     */
    public static void addFolderToZip(ZipFile zipFile, File file, ZipParameters parameters, @Nullable FileFilter fileFilter) throws ZipException {
        if (file == null || parameters == null) {
            throw new ZipException("one of the input parameters is null, cannot add folder to zip");
        }

        if (!Zip4jUtil.checkFileExists(file.getAbsolutePath())) {
            throw new ZipException("input folder does not exist");
        }

        if (!file.isDirectory()) {
            throw new ZipException("input file is not a folder, user addFileToZip method to add files");
        }

        if (!Zip4jUtil.checkFileReadAccess(file.getAbsolutePath())) {
            throw new ZipException("cannot read folder: " + file.getAbsolutePath());
        }

        String rootFolderPath = null;
        if (parameters.isIncludeRootFolder()) {
            if (file.getAbsolutePath() != null) {
                rootFolderPath = file.getAbsoluteFile().getParentFile() != null ? file.getAbsoluteFile().getParentFile().getAbsolutePath() : "";
            } else {
                rootFolderPath = file.getParentFile() != null ? file.getParentFile().getAbsolutePath() : "";
            }
        } else {
            rootFolderPath = file.getAbsolutePath();
        }

        parameters.setDefaultFolderPath(rootFolderPath);

        ArrayList<File> fileList = (ArrayList<File>) Zip4jUtil.getFilesInDirectoryRec(file, parameters.isReadHiddenFiles());

        if (parameters.isIncludeRootFolder()) {
            if (fileList == null) {
                fileList = new ArrayList<>();
            }
            fileList.add(file);
        }

        ArrayList<File> filterArrayList;
        if (fileFilter != null) {
            filterArrayList = new ArrayList<>();
            for (File targetFile : fileList) {
                if (fileFilter.accept(targetFile)) {
                    filterArrayList.add(targetFile);
                }
            }
        } else {
            filterArrayList = fileList;
        }
        zipFile.addFiles(filterArrayList, parameters);
    }

}