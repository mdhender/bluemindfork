package net.bluemind.lib.vertx.internal;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.IVerticlePriority;

public class BMModule extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(BMModule.class);

	public BMModule() {
		logger.info("BM module created.");
	}

	public void start(final Promise<Void> future) {
		logger.info("Starting...");
		RunnableExtensionLoader<IVerticleFactory> vfLoader = new RunnableExtensionLoader<IVerticleFactory>();
		List<IVerticleFactory> factos = vfLoader.loadExtensions("net.bluemind.lib.vertx", "verticles", "verticle",
				"impl");

		// sort verticle factories by priority
		Collections.sort(factos, (IVerticleFactory o1, IVerticleFactory o2) -> {
			int priority1 = 0;
			int priority2 = 0;
			if (o1 instanceof IVerticlePriority) {
				priority1 = ((IVerticlePriority) o1).getPriority();
			}

			if (o2 instanceof IVerticlePriority) {
				priority2 = ((IVerticlePriority) o2).getPriority();
			}

			return priority2 - priority1;

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
					logger.error("verticle loading failed: {}", t.getMessage(), t);
				}
				deploy(queue.poll(), future, this);
			}
		};
		IVerticleFactory firstOne = queue.poll();
		logger.info("============ SPAWN THE VERTICLES, starting with {} =========", firstOne);
		deploy(firstOne, future, oneByOne);
	}

	private void deploy(IVerticleFactory vf, Promise<Void> future, final Handler<AsyncResult<String>> done) {
		if (vf == null) {
			logger.info("============ VERTICLES SPAWNED =========");
			future.complete();
			return;
		}
		Supplier<Verticle> vc = fromFactory(vf);
		Vertx vx = getVertx();

		logger.info("deploying {} verticle {}", vf.isWorker() ? "worker" : "std", vf);
		if (vf.isWorker()) {
			DeploymentOptions workerOpts = new DeploymentOptions().setInstances(1).setWorker(true);
			if (vf instanceof IUniqueVerticleFactory) {
				vx.deployVerticle(vc, workerOpts, done);
			} else {
				vx.deployVerticle(vc, workerOpts.setMultiThreaded(true), done);
			}
		} else {

			if (vf instanceof IUniqueVerticleFactory) {
				vx.deployVerticle(vc, new DeploymentOptions().setInstances(1), done);
			} else {
				vx.deployVerticle(vc,
						new DeploymentOptions().setInstances(Runtime.getRuntime().availableProcessors() * 2), done);
			}
		}
	}

	private Supplier<Verticle> fromFactory(IVerticleFactory vf) {
		return () -> vf.newInstance();
	}

	public void stop() {
		logger.info("Stopping...");
	}

}
