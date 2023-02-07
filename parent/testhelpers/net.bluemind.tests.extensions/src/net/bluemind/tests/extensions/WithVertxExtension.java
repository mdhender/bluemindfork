package net.bluemind.tests.extensions;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.vertx.VertxPlatform;

public class WithVertxExtension implements BeforeAllCallback {

	private static final Logger logger = LoggerFactory.getLogger(WithVertxExtension.class);

	@Override
	public void beforeAll(ExtensionContext arg0) throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

}
