package com.surcumference.fingerprint.util;

import android.os.Build;

import com.surcumference.fingerprint.util.log.L;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogcatManager {
	private Process process;
	private final File targetFile;
	private ScheduledExecutorService timeOutExecutor = Executors.newScheduledThreadPool(1);

	public LogcatManager(File targetFile) {
		this.targetFile = targetFile;
	}

	public void startLogging(long timeOutMS) {
		synchronized (LogcatManager.class) {
			try {
				stopLoggingInternal();
				this.process = Runtime.getRuntime().exec("logcat");
				Task.onBackground(() -> {
					PrintWriter pw = null;
					try {
						pw = new PrintWriter(targetFile);
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line;
						while ((line = bufferedReader.readLine()) != null) {
							pw.println(line);
						}
					} catch (Exception e) {
						L.e(e);
					} finally {
						FileUtils.closeCloseable(pw);
					}
				});
				timeOutExecutor.shutdown();
				timeOutExecutor = Executors.newScheduledThreadPool(1);
				timeOutExecutor.schedule(() -> {
					stopLoggingInternal();
					L.d("stopLogging on timeout");
				}, timeOutMS, TimeUnit.MILLISECONDS);
				L.d("startLogging timeOutMS", timeOutMS);
			} catch (Exception e) {
				L.e(e);
			}
		}
	}

	public void stopLogging() {
		synchronized (LogcatManager.class) {
			stopLoggingInternal();
			L.d("stopLogging");
		}
	}

	private void stopLoggingInternal() {
		try {
			if (!isRunning()) {
				return;
			}
			int pid = getPid(process);
			if (pid > -1) {
				android.os.Process.killProcess(pid);
			}
			process.destroyForcibly();
		} catch (Exception e) {
			L.e(e);
		} finally {
			timeOutExecutor.shutdown();
			timeOutExecutor = Executors.newScheduledThreadPool(1);
		}
	}

	public boolean isRunning() {
		Process process = this.process;
		if (process == null) {
			return false;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return process.isAlive();
		}
		return new File("/proc/"+ getPid(process)).exists();
	}

	public File getTargetFile() {
		return this.targetFile;
	}

	public static int getPid(Process p) {
		try {
			Field f = p.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			return f.getInt(p);
		} catch (Throwable e) {
			L.e(e);
		}
		return -1;
	}
}