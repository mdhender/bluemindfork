package net.bluemind.lib.vertx.internal;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.Verticle;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.IVerticlePriority;

public class BMModule extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(BMModule.class);

	public BMModule() {
		logger.info("BM module created.");
	}

	public void start(final Future<Void> future) {
		logger.info("Starting...");
		RunnableExtensionLoader<IVerticleFactory> vfLoader = new RunnableExtensionLoader<IVerticleFactory>();
		List<IVerticleFactory> factos = vfLoader.loadExtensions("net.bluemind.lib.vertx", "verticles", "verticle",
				"impl");

		// sort verticle factories by priority
		Collections.sort(factos, new Comparator<IVerticleFactory>() {

			@Override
			public int compare(IVerticleFactory o1, IVerticleFactory o2) {
				int priority1 = 0;
				int priority2 = 0;
				if (o1 instanceof IVerticlePriority) {
					priority1 = ((IVerticlePriority) o1).getPriority();
				}

				if (o2 instanceof IVerticlePriority) {
					priority2 = ((IVerticlePriority) o2).getPriority();
				}

				int diff = priority2 - priority1;
				return diff;
			}
		});

		logger.debug("start factories in this order");
		int order = 1;
		for (IVerticleFactory factory : factos) {
			logger.debug("{}:{}", order, factory.getClass().getSimpleName());
			order++;
		}
		final ArrayDeque<IVerticleFactory> queue = new ArrayDeque<IVerticleFactory>(factos);
		Handler<AsyncResult<String>> oneByOne = new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				if (event.failed()) {
					Throwable t = event.cause();
					logger.error(t.getMessage(), t);
					// System.exit(1);
				}
				deploy(queue.poll(), future, this);
			}
		};
		IVerticleFactory firstOne = queue.poll();
		logger.info("============ SPAWN THE VERTICLES, starting with {} =========", firstOne);
		deploy(firstOne, future, oneByOne);
	}

	private void deploy(IVerticleFactory vf, Future<Void> future, final Handler<AsyncResult<String>> done) {
		if (vf == null) {
			logger.info("============ VERTICLES SPAWNED =========");
			future.setResult(null);
			return;
		}

		final String klass = vf.newInstance().getClass().getCanonicalName();

		logger.info("deploying {} verticle {}", vf.isWorker() ? "worker" : "std", klass);
		if (vf.isWorker()) {
			if (vf instanceof IUniqueVerticleFactory) {
				container.deployWorkerVerticle(klass, null, 1, false, done);
			} else {
				container.deployWorkerVerticle(klass, null, 1, true, done);
			}
		} else {

			if (vf instanceof IUniqueVerticleFactory) {
				container.deployVerticle(klass, 1, done);
			} else {
				// deploy 1, then deploy others so that osgi bundles are
				// resolved
				// when the first one is loaded
				container.deployVerticle(klass, 1, new Handler<AsyncResult<String>>() {

					@Override
					public void handle(AsyncResult<String> event) {
						if (event.succeeded()) {
							int inst = Runtime.getRuntime().availableProcessors() * 2 - 1;
							logger.info("********* Time for {} more {}.... ", inst, klass);
							container.deployVerticle(klass, inst - 1, done);
						} else {
							Throwable t = event.cause();
							logger.error("Load Error: " + t.getMessage(), t);
							// System.exit(1);
						}
					}
				});
			}
		}
	}

	public void stop() {
		logger.info("Stopping...");
	}

}
