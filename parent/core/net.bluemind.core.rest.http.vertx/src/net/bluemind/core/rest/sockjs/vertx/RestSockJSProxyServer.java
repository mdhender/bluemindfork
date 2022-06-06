package net.bluemind.core.rest.sockjs.vertx;

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

	public RestSockJSProxyServer(Vertx vertx, IRestCallHandler proxy, IRestBusHandler restbus) {
		this.vertx = vertx;
		this.proxy = proxy;
		this.restbus = restbus;
	}

	@Override
	public void handle(SockJSSocket sock) {
		io.vertx.core.parsetools.JsonParser jsonParser = io.vertx.core.parsetools.JsonParser.newParser()
				.objectValueMode();
		RestSockJsProxyHandler client = new RestSockJsProxyHandler(vertx, sock, proxy, restbus);

		sock.exceptionHandler((Throwable e) -> {
			logger.error("error in sock {}: {}", sock, e.getMessage());
			sock.close();
		});
		sock.endHandler(v -> handleSocketClosed(client));

		jsonParser.exceptionHandler(t -> handleSocketClosed(client));
		jsonParser.handler(client);
		sock.handler(jsonParser);
	}

	protected void handleSocketClosed(RestSockJsProxyHandler client) {
		client.close();
	}

}
