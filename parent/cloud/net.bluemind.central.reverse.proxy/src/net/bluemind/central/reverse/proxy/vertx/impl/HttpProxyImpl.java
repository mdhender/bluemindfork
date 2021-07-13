package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import net.bluemind.central.reverse.proxy.vertx.HttpProxy;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.ProxyRequest;
import net.bluemind.central.reverse.proxy.vertx.ProxyResponse;

public class HttpProxyImpl implements HttpProxy {

	private final Logger logger = LoggerFactory.getLogger(HttpProxyImpl.class);

	private final HttpClient client;
	private Function<HttpServerRequestContext, Future<SocketAddress>> targetSelector = context -> Future
			.failedFuture("No target available");

	private BiConsumer<HttpServerRequestContext, ProxyResponse> responseHook = (req, res) -> {
	};

	public HttpProxyImpl(HttpClient client) {
		this.client = client;
	}

	@Override
	public HttpProxy target(int port, String host) {
		return target(new SocketAddressImpl(port, host));
	}

	@Override
	public HttpProxy target(SocketAddress address) {
		targetSelector = context -> Future.succeededFuture(address);
		return this;
	}

	@Override
	public HttpProxy selector(Function<HttpServerRequestContext, Future<SocketAddress>> selector) {
		targetSelector = selector;
		return this;
	}

	@Override
	public HttpProxy responseHook(BiConsumer<HttpServerRequestContext, ProxyResponse> responseHook) {
		this.responseHook = responseHook;
		return this;
	}

	@Override
	public ProxyRequest proxy(HttpServerRequest request, SocketAddress target) {
		return new ProxyRequestImpl(client, target, new HttpServerRequestContextImpl(request));
	}

	@Override
	public void handle(HttpServerRequest request) {
		doReq(request);
	}

	private void doReq(HttpServerRequest request) {
		request.pause();
		HttpServerRequestContext context = new HttpServerRequestContextImpl(request);
		Future<SocketAddress> futureTarget = targetSelector.apply(context);
		futureTarget.onComplete(ar -> {
			if (ar.succeeded()) {
				SocketAddress address = ar.result();
				ProxyRequest proxyReq = new ProxyRequestImpl(client, address, context);
				proxyReq.send(ar1 -> {
					if (ar1.succeeded()) {
						ProxyResponse proxyResp = ar1.result();
						responseHook.accept(context, proxyResp);
						proxyResp.send(ar2 -> {
							// Done
						});
					}
				});
			} else {
				request.resume();
				request.response().setStatusCode(404).end();
			}
		});
	}
}