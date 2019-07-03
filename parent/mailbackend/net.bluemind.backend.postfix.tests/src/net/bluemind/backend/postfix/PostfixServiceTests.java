package net.bluemind.backend.postfix;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.lib.vertx.VertxPlatform;

public class PostfixServiceTests {
	@Before
	public void before() throws Exception {
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@Test
	public void reInitializeAllMaps() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new PostfixService().reInitializeAllMaps();

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}
}
