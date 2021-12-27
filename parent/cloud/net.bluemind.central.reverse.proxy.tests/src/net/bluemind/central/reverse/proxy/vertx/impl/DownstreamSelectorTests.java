package net.bluemind.central.reverse.proxy.vertx.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.mockito.Mockito;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;

public class DownstreamSelectorTests {

	@Test
	public void testLoginRouteSelection() throws InterruptedException {

		ProxyInfoStoreClient storeClient = Mockito.mock(ProxyInfoStoreClient.class);
		when(storeClient.ip("one")).thenReturn(Future.succeededFuture("1.1.1.1"));
		when(storeClient.ip("two")).thenReturn(Future.succeededFuture("2.2.2.2"));

		DownstreamSelector<HttpServerRequestContext> selector = new DownstreamSelector<>(new RequestInfoMatcher(),
				storeClient);

		final CountDownLatch cdl = new CountDownLatch(2);

		MultiMap formAttributes = MultiMap.caseInsensitiveMultiMap().add("login", "one");
		HttpServerRequest request = TestRequestHelper.createRequest(HttpMethod.POST, "/login", formAttributes);

		selector.apply(new HttpServerRequestContextImpl(request)).onSuccess(socketAddress -> {
			assertEquals("1.1.1.1", socketAddress.host());
			cdl.countDown();
		});

		formAttributes = MultiMap.caseInsensitiveMultiMap().add("login", "two");
		request = TestRequestHelper.createRequest(HttpMethod.POST, "/login", formAttributes);

		selector.apply(new HttpServerRequestContextImpl(request)).onSuccess(socketAddress -> {
			assertEquals(socketAddress.host(), "2.2.2.2");
			cdl.countDown();
		});

		cdl.await();
	}

	@Test
	public void testAnonymousRouteSelection() throws InterruptedException {

		ProxyInfoStoreClient storeClient = Mockito.mock(ProxyInfoStoreClient.class);
		when(storeClient.ip(anyString())).thenReturn(Future.succeededFuture());
		when(storeClient.anyIp()).thenReturn(Future.succeededFuture("1.1.1.1"), Future.succeededFuture("2.2.2.2"));

		DownstreamSelector<HttpServerRequestContext> selector = new DownstreamSelector<>(new RequestInfoMatcher(),
				storeClient);

		final CountDownLatch cdl = new CountDownLatch(2);
		HttpServerRequest request = TestRequestHelper.createRequest(HttpMethod.GET, "/", null);
		selector.apply(new HttpServerRequestContextImpl(request)).onSuccess(socketAddress -> {
			assertEquals("1.1.1.1", socketAddress.host());
			cdl.countDown();
		});
		selector.apply(new HttpServerRequestContextImpl(request)).onSuccess(socketAddress -> {
			assertEquals(socketAddress.host(), "2.2.2.2");
			cdl.countDown();
		});
		cdl.await();
	}

}
