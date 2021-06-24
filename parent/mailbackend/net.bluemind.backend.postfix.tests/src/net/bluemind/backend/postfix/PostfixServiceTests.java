package net.bluemind.backend.postfix;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.lib.vertx.VertxPlatform;

public class PostfixServiceTests {
	@Before
	public void before() throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void reInitializeAllMaps() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new PostfixService().reInitializeAllMaps();

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}
}
