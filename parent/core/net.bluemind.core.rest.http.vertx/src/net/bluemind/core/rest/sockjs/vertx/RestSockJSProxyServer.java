package net.bluemind.core.rest.sockjs.vertx;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import net.bluemind.core.rest.base.IRestBusHandler;
import net.bluemind.core.rest.base.IRestCallHandler;

public class RestSockJSProxyServer implements Handler<SockJSSocket> {

	private static final Logger logger = LoggerFactory.getLogger(RestSockJSProxyServer.class);
	private Vertx vertx;
	private IRestCallHandler proxy;
	private IRestBusHandler restbus;
	private Set<RestSockJsProxyHandler> clients = new HashSet<>();

	public RestSockJSProxyServer(Vertx vertx, IRestCallHandler proxy, IRestBusHandler restbus) {
		this.vertx = vertx;
		this.proxy = proxy;
		this.restbus = restbus;
	}

	@Override
	public void handle(SockJSSocket sock) {
		RestSockJsProxyHandler client = new RestSockJsProxyHandler(vertx, sock, proxy, restbus);
		clients.add(client);

		logger.debug("connected clients {}", clients.size());

		sock.exceptionHandler((Throwable e) -> {
			logger.error("error in sock {}: {}", sock, e);
			sock.close();
		});
		sock.endHandler(v -> {
			handleSocketClosed(client);
		});

		sock.handler(client);
	}

	protected void handleSocketClosed(RestSockJsProxyHandler client) {
		client.close();
		clients.remove(client);
		logger.debug("socket closed, connected clients {}", clients.size());
	}

}
