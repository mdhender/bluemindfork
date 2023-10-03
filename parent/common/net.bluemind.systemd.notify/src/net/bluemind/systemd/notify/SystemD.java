package net.bluemind.systemd.notify;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jna.Library; // NOSONAR
import com.sun.jna.Native; // NOSONAR

public class SystemD {

	private static final Logger logger = LoggerFactory.getLogger(SystemD.class);
	private static final Api INSTANCE = init();

	private static final CLibrary LIBC = Native.load("c", CLibrary.class);

	private interface CLibrary extends Library {

		int getpid();
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

	@VisibleForTesting
	public static boolean isAvailable() {
		return INSTANCE != null;
	}

	@VisibleForTesting
	public static Api get() {
		if (!isAvailable()) {
			throw new SystemDException("SystemD is not available");
		}
		return INSTANCE;
	}

	private static Api init() {
		try {
			RawApi rawApi = Native.load("systemd", RawApi.class);
			return new Api(rawApi);
		} catch (UnsatisfiedLinkError le) {
			logger.warn("systemd library not found: {}", le.getMessage());
			return null;
		}
	}

}
