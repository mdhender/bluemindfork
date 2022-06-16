package net.bluemind.central.reverse.proxy.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStorage;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStore;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.lib.vertx.VertxPlatform;

public class ProxyInfoStoreClientImplTests {

	private Vertx vertx;
	private ProxyInfoStorage storage;
	private ProxyInfoStore store;

	@Before
	public void setupTest() {
		vertx = VertxPlatform.getVertx();
		Set<String> domainAliases = new HashSet<>();
		domainAliases.addAll(Arrays.asList("alias1", "alias2"));
		storage = ProxyInfoStorage.create();
		storage.addDomain(".internal", domainAliases);
		storage.addLogin("one@alias1", "here1");
		storage.addLogin("two@alias2", "here2");
		storage.addDataLocation("here1", "ip1");
		storage.addDataLocation("here2", "ip2");
		store = ProxyInfoStore.create(vertx, storage);
		store.setupService();
	}

	@After
	public void tearDownTest() {
		store.tearDown();
	}

	@Test
	public void noStore() {
		store.tearDown();
		ProxyInfoStoreClient client = ProxyInfoStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> {
			client.ip("one@alias1").onComplete(ar -> {
				context.assertions(() -> {
					assertTrue(ar.failed());
				});
			});
		});
	}

	@Test
	public void ip() {
		ProxyInfoStoreClient client = ProxyInfoStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> {
			client.ip("one@alias1").onComplete(ar -> {
				context.assertions(() -> {
					assertTrue(ar.succeeded());
					assertEquals("ip1", ar.result());
				});
			});
		});
	}

	@Test
	public void ipWithInexistantLogin() {
		ProxyInfoStoreClient client = ProxyInfoStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> {
			client.ip("dontexists").onComplete(ar -> {
				context.assertions(() -> {
					assertTrue(ar.failed());
				});
			});
		});
	}

	@Test
	public void anyIps() {
		ProxyInfoStoreClient client = ProxyInfoStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> {
			client.anyIp().onComplete(ar -> {
				context.assertions(() -> {
					assertTrue(ar.succeeded());
					assertTrue(Arrays.asList("ip1", "ip2").contains(ar.result()));
				});
			});
		});
	}

	@Test
	public void addLogin() {
		ProxyInfoStoreClient client = ProxyInfoStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> {
			List<DirEmail> emails = Arrays.asList(new DirEmail("three@alias1", false));
			DirInfo dir = new DirInfo(".internal", emails, "here1");
			client.addDir(dir).compose(v -> client.ip("three@alias1")).onComplete(ar -> {
				context.assertions(() -> {
					assertTrue(ar.succeeded());
					assertEquals("ip1", ar.result());
				});
			});
		});
	}

	@Test
	public void addDataLocation() {
		ProxyInfoStoreClient client = ProxyInfoStoreClient.create(vertx);
		AsyncTestContext.asyncTest(context -> {
			client.addInstallation(new InstallationInfo("here1", "elsewere")).compose(v -> client.ip("one@alias1"))
					.onComplete(ar -> {
						context.assertions(() -> {
							assertTrue(ar.succeeded());
							assertEquals("elsewere", ar.result());
						});
					});
		});
	}

}
