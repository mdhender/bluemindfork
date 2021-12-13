package net.bluemind.tika.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import net.bluemind.lib.vertx.RouteMatcher;

public final class ReceiveDocumentVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveDocumentVerticle.class);
	private HttpServer srv;

	public ReceiveDocumentVerticle() {
		logger.info("created");
	}

	@Override
	public void start() {
		HttpServerOptions opts = new HttpServerOptions();
		opts.setAcceptBacklog(1024).setReuseAddress(true);
		opts.setTcpNoDelay(true);
		this.srv = vertx.createHttpServer(opts);

		RouteMatcher rm = createRouter();
		srv.requestHandler(rm).listen(8087, server -> {
			logger.info("Bound to 8087");
		});
	}

	private RouteMatcher createRouter() {
		RouteMatcher rm = new RouteMatcher(vertx);
		rm.post("/tika", new TikaPostHandler(vertx));
		rm.get("/tika/:hash/", new TikaGetHashHandler(vertx));
		return rm;
	}

}
