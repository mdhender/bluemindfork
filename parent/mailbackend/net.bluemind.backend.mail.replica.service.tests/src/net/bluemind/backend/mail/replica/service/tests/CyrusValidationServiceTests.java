package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.ICyrusValidation;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;

public class CyrusValidationServiceTests {
	private SecurityContext domainAdminSecurityContext;

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Void> startResult = new CompletableFuture<>();
		VertxPlatform.spawnVerticles(spawnResult -> {
			if (spawnResult.succeeded()) {
				startResult.complete(null);
			} else {
				startResult.completeExceptionally(spawnResult.cause());
			}
		});
		startResult.get(20, TimeUnit.SECONDS);
	}

	private ICyrusValidation getService(SecurityContext context) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICyrusValidation.class);
	}

	@Test
	public void emailDefaultIsNotValid() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate("default", "");
		assertFalse(result);
	}

	@Test(expected = ServerFault.class)
	public void emailNullIsNotValid() {
		ICyrusValidation cli = getService(domainAdminSecurityContext);
		boolean result = cli.prevalidate(null, "");
		assertFalse(result);
	}
}
