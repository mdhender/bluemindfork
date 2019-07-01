package net.bluemind.tika.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public final class ReceiveDocumentVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveDocumentVerticle.class);
	private HttpServer srv;

	public ReceiveDocumentVerticle() {
		logger.info("created");
	}

	public void start() {
		this.srv = vertx.createHttpServer();
		srv.setAcceptBacklog(1024).setReuseAddress(true);
		srv.setTCPNoDelay(true);
		srv.setUsePooledBuffers(true);

		RouteMatcher rm = createRouter();
		srv.requestHandler(rm).listen(8087, new Handler<AsyncResult<HttpServer>>() {

			@Override
			public void handle(AsyncResult<HttpServer> event) {
				logger.info("Bound to 8087");
			}
		});
	}

	private RouteMatcher createRouter() {
		RouteMatcher rm = new RouteMatcher();
		rm.post("/tika", new TikaPostHandler(vertx));
		rm.get("/tika/:hash/", new TikaGetHashHandler(vertx));
		return rm;
	}

}
