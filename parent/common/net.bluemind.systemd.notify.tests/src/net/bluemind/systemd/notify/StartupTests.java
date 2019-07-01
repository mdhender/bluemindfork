package net.bluemind.systemd.notify;

import static org.junit.Assert.fail;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import net.bluemind.vertx.testhelper.Deploy;

public class StartupTests {

	public static class WithSystemD implements TestRule {

		@Override
		public Statement apply(Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					if (!SystemD.isAvailable()) {
						throw new AssumptionViolatedException("systemd not available. Skipping test!");
					} else {
						base.evaluate();
					}
				}
			};
		}

	}

	@ClassRule
	public static WithSystemD withSystemD = new WithSystemD();

	private Set<String> deployed;

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException {
		this.deployed = Deploy.verticles(true, NotifyStartupVerticle.class.getCanonicalName()).get(5, TimeUnit.SECONDS);
	}

	@After
	public void after() {
		Deploy.afterTest(deployed);
	}

	@Test
	public void testStart() {
		CompletableFuture<Void> promise = Startup.notifyReady();
		try {
			promise.get(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
