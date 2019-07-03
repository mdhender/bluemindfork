package net.bluemind.lib.vertx.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.PlatformManager;
import org.vertx.java.platform.PlatformManagerFactory;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

public class BMPlatformManagerFactory implements PlatformManagerFactory {

	private static final Logger logger = LoggerFactory.getLogger(BMPlatformManagerFactory.class);
	private final BMPlatformManager pm;

	public BMPlatformManagerFactory() {
		logger.info("Using BM platform manager factory.");
		if (Thread.currentThread().getContextClassLoader() == null) {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		} else {
			logger.info("Classloader used by vertx {}, factory classloader {}",
					Thread.currentThread().getContextClassLoader(), this.getClass().getClassLoader());
		}
		InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

		this.pm = new BMPlatformManager();
	}

	@Override
	public PlatformManager createPlatformManager() {
		return pm;
	}

	@Override
	public PlatformManager createPlatformManager(int clusterPort, String clusterHost) {
		return pm;
	}

	@Override
	public PlatformManager createPlatformManager(int clusterPort, String clusterHost, int quorumSize, String haGroup) {
		return pm;
	}

}
