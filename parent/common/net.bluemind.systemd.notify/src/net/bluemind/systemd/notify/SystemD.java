package net.bluemind.systemd.notify;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jna.Library; // NOSONAR
import com.sun.jna.Native; // NOSONAR

public class SystemD {

	private static final Logger logger = LoggerFactory.getLogger(SystemD.class);
	private static final Api INSTANCE = init();

	private static final CLibrary LIBC = (CLibrary) Native.loadLibrary("c", CLibrary.class);

	private interface CLibrary extends Library {

		int getpid();
	}

	public static enum SystemDLocation {
		Centos("/usr/lib64/libsystemd.so.0"),

		// 16.04, 18.04, stretch
		OldUbuntuDebian("/lib/x86_64-linux-gnu/libsystemd.so.0"),

		// /lib link to /usr/lib
		UbuntuDebian("/usr/lib/x86_64-linux-gnu/libsystemd.so.0");

		public final String systemdLibLocation;

		private SystemDLocation(String lib) {
			this.systemdLibLocation = lib;
		}
	}

	public static class Api {

		private final RawApi impl;

		private Api(RawApi rawApi) {
			this.impl = rawApi;
		}

		public void notifyReady() {
			int pid = LIBC.getpid();
			logger.info("Notify ready through systemd for PID {}...", pid);
			int errorCode = impl.sd_pid_notify(pid, 0, "READY=1");
			if (errorCode <= 0) {
				logger.error("Notify failed: {}", errorCode);
			} else {
				logger.info("Notified for {}, errorCode: {}", pid, errorCode);
			}
		}

		public void setupWatchdog(long period, TimeUnit unit) {
			int pid = LIBC.getpid();
			logger.info("Setup systemd watchdog for PID {}...", pid);
			int errorCode = impl.sd_pid_notify(pid, 0, "WATCHDOG_USEC=" + unit.toMicros(period));
			if (errorCode <= 0) {
				logger.error("setupWatchdog failed: {}", errorCode);
			} else {
				logger.info("setupWatchdog OK for {}, errorCode: {}", pid, errorCode);
			}
		}

		public void watchdogKeepalive() {
			int pid = LIBC.getpid();
			logger.info("keepAlive for PID {}...", pid);
			int errorCode = impl.sd_pid_notify(pid, 0, "WATCHDOG=1");
			if (errorCode <= 0) {
				logger.error("keepAlive failed: {}", errorCode);
			} else {
				logger.debug("keepAlive for {}, errorCode: {}", pid, errorCode);
			}
		}

	}

	private interface RawApi extends Library {

		int sd_pid_notify(int pid, int unset, String state); // NOSONAR

	}

	private static SystemDLocation figureOutLocation() {
		for (SystemDLocation loc : SystemDLocation.values()) {
			if (new File(loc.systemdLibLocation).exists()) {
				logger.info("Selected location {}", loc);
				return loc;
			}
		}
		return null;
	}

	@VisibleForTesting
	public static boolean isAvailable() {
		if (INSTANCE == null) {
			return false;
		}
		return true;
	}

	@VisibleForTesting
	public static Api get() {
		if (!isAvailable()) {
			throw new SystemDException("SystemD is not available");
		}
		return INSTANCE;
	}

	private static Api init() {
		SystemDLocation loc = figureOutLocation();
		if (loc != null) {
			RawApi rawApi = (RawApi) Native.loadLibrary(loc.systemdLibLocation, RawApi.class);
			return new Api(rawApi);
		}
		return null;
	}

}
