package net.bluemind.tika.server;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.PlatformManager;

import net.bluemind.lib.vertx.Constructor;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.SystemD;
import net.bluemind.tika.server.impl.ExtractTextWorker;
import net.bluemind.tika.server.impl.ReceiveDocumentVerticle;
import net.bluemind.tika.server.impl.SystemdWatchdogVerticle;

public class TikaServer implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(TikaServer.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting...");

		File[] toDelete = new File(System.getProperty("java.io.tmpdir")).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isFile()) {
					String fn = pathname.getName();
					if (fn.startsWith("tika") && fn.endsWith(".bin")) {
						return true;
					}

					if (fn.startsWith("apache-tika-") && fn.endsWith(".tmp")) {
						return true;
					}
				}
				return false;
			}
		});
		for (File f : toDelete) {
			f.delete();
		}

		PlatformManager pm = VertxPlatform.getPlatformManager();
		CountDownLatch cdl = new CountDownLatch(2);
		Handler<AsyncResult<String>> doneHandler = new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()) {
					logger.info("Deployement done with id: {}", event.result());
					cdl.countDown();
				} else {
					logger.error("Deployement failed", event.cause());
				}
			}
		};

		pm.deployVerticle(Constructor.of(ReceiveDocumentVerticle::new, ReceiveDocumentVerticle.class), null, new URL[0],
				32, null, doneHandler);

		pm.deployWorkerVerticle(false, Constructor.of(ExtractTextWorker::new, ExtractTextWorker.class), null,
				new URL[0], 4, null, doneHandler);
		cdl.await(1, TimeUnit.MINUTES);
		if (SystemD.isAvailable()) {
			SystemD.get().notifyReady();
			pm.deployVerticle(Constructor.of(SystemdWatchdogVerticle::new, SystemdWatchdogVerticle.class), null,
					new URL[0], 1, null, ar -> {
						if (ar.failed()) {
							logger.error("Watchdog setup failed", ar.cause());
						}
					});

		}

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("Stopped.");
	}

}
