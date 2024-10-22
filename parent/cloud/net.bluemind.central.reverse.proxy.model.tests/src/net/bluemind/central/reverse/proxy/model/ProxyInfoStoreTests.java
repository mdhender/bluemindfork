package net.bluemind.central.reverse.proxy.model;

import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ActionHeader;
import net.bluemind.central.reverse.proxy.model.impl.AsyncTestContext;
import net.bluemind.lib.vertx.VertxPlatform;

public class ProxyInfoStoreTests {

	private final Logger logger = LoggerFactory.getLogger(ProxyInfoStoreTests.class);

	private Vertx vertx;
	private ProxyInfoStorage storage;
	private ProxyInfoStore store;

	@Before
	public void setupTest() {
		vertx = VertxPlatform.getVertx();
		storage = Mockito.spy(ProxyInfoStorage.class);
		store = ProxyInfoStore.create(storage);
		store.setupService(vertx);
	}

	@After
	public void tearDownTest() throws InterruptedException, ExecutionException {
		store.tearDown();
	}

	@Test
	public void testAddLogin() {
		AsyncTestContext.asyncTest(context -> {
			JsonObject emails = new JsonObject().put("address", "anyLogin").put("allAliases", false);
			JsonObject json = new JsonObject().put("emails", new JsonArray().add(emails))
					.put("dataLocation", "anyDataLocation").put("domainUid", "anyDomains");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.ADD_DIR.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.succeeded());
						assertNull(ar.result().body());
						verify(storage, times(1)).addLogin("anyLogin", "anyDataLocation");
					}));
		});
	}

	@Test
	public void testAddLogin_withWrongParameterName() {
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("wrongProperty", "value").put("dataLocation", "anyDataLocation");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.ADD_DIR.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.failed());
						assertTrue(ar.cause() instanceof ReplyException);
						assertEquals(500, ((ReplyException) ar.cause()).failureCode());
						verify(storage, times(0)).addLogin(any(), any());
					}));
		});
	}

	@Test
	public void testAddLogin_withWrongParameterValue() {
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("emails", new JsonObject()).put("dataLocation", "anyDataLocation");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.ADD_DIR.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.failed());
						assertTrue(ar.cause() instanceof ReplyException);
						assertEquals(500, ((ReplyException) ar.cause()).failureCode());
						verify(storage, times(0)).addLogin(any(), any());
					}));
		});
	}

	@Test
	public void testIp() {
		org.mockito.Mockito.when(storage.ip(Mockito.anyString())).thenReturn("1.2.3.4");
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("login", "any");
			vertx.eventBus().<JsonObject>request(ADDRESS, json, ActionHeader.IP.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.succeeded());
						assertEquals("1.2.3.4", ar.result().body().getString("ip"));
						verify(storage, times(1)).ip("any");
					}));
		});
	}

	@Test
	public void testIp_withNonExistantIp() {
		org.mockito.Mockito.when(storage.ip(Mockito.anyString())).thenReturn(null);
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("login", "any");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.IP.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.failed());
						assertTrue(ar.cause() instanceof ReplyException);
						assertEquals(404, ((ReplyException) ar.cause()).failureCode());
						verify(storage, times(1)).ip("any");
					}));
		});
	}

	@Test
	public void testIp_withWrongParameterName() {
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("wrongProperty", "any");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.IP.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.failed());
						assertTrue(ar.cause() instanceof ReplyException);
						assertEquals(500, ((ReplyException) ar.cause()).failureCode());
						verify(storage, times(0)).ip(any());
					}));
		});
	}

	@Test
	public void testAnyIp() {
		org.mockito.Mockito.when(storage.anyIp()).thenReturn("1.2.3.4");
		AsyncTestContext.asyncTest(context -> {
			vertx.eventBus().<JsonObject>request(ADDRESS, null, ActionHeader.ANY_IP.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.succeeded());
						assertEquals("1.2.3.4", ar.result().body().getString("ip"));
						verify(storage, times(1)).anyIp();
					}));
		});
	}

	@Test
	public void testAnyIp_whenNoIpAvailable() {
		org.mockito.Mockito.when(storage.anyIp()).thenReturn(null);
		AsyncTestContext.asyncTest(context -> {
			vertx.eventBus().request(ADDRESS, null, ActionHeader.ANY_IP.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.failed());
						assertTrue(ar.cause() instanceof ReplyException);
						assertEquals(404, ((ReplyException) ar.cause()).failureCode());
						verify(storage, times(1)).anyIp();
					}));
		});
	}

	@Test
	public void testAddDataLocation() {
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("dataLocation", "anyDataLocation").put("ip", "1.2.3.4");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.ADD_INSTALLATION.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						if (ar.failed()) {
							ar.cause().printStackTrace();
						}
						assertTrue(ar.succeeded());
						assertNull(ar.result().body());
						verify(storage, times(1)).addDataLocation("anyDataLocation", "1.2.3.4");
					}));
		});
	}

	@Test
	public void testAddDataLocation_withWrongParameterName() {
		AsyncTestContext.asyncTest(context -> {
			JsonObject json = new JsonObject().put("wrongProperty", "anyDataLocation").put("ip", "1.2.3.4");
			vertx.eventBus().request(ADDRESS, json, ActionHeader.ADD_INSTALLATION.getDeliveryOptions(),
					ar -> context.assertions(() -> {
						assertTrue(ar.failed());
						assertTrue(ar.cause() instanceof ReplyException);
						assertEquals(500, ((ReplyException) ar.cause()).failureCode());
						verify(storage, times(0)).addDataLocation(any(), any());
					}));
		});
	}

}
