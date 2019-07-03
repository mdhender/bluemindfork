package net.bluemind.hps.auth.core2.tests;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.hps.auth.core2.C2ProviderFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.ILogoutListener;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class C2ProviderTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final CountDownLatch cdl = new CountDownLatch(1);
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				cdl.countDown();
			}
		};
		VertxPlatform.spawnVerticles(done);
		cdl.await();

		PopulateHelper.initGlobalVirt();

		PopulateHelper.addDomainAdmin("admin0", "global.virt");

	}

	public static class TestListener implements ILogoutListener {

		@Override
		public void loggedOut(String sessionId) {
			// TODO Auto-generated method stub

		}

		@Override
		public void loggedOutAll() {
			// TODO Auto-generated method stub

		}

	}

	@Test
	public void testNewSession() throws InterruptedException {
		C2ProviderFactory c2pf = new C2ProviderFactory();
		TestListener tl = new TestListener();
		c2pf.setLogoutListener(tl);
		IAuthProvider provider = c2pf.get(VertxPlatform.getVertx());
		Assert.assertNotNull(provider);
		final BlockingQueue<String> queue = new LinkedBlockingDeque<>();
		provider.sessionId("admin0@global.virt", "admin", true, Collections.emptyList(), new AsyncHandler<String>() {

			@Override
			public void success(String value) {
				queue.offer(value);
			}

			@Override
			public void failure(Throwable e) {
				e.printStackTrace();
			}

		});

		String sessionId = queue.poll(1000, TimeUnit.MILLISECONDS);
		System.err.println("sessionid " + sessionId);
		Assert.assertNotNull(sessionId);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

}
