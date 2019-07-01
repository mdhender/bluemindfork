package net.bluemind.tika.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;

public class TikaPostHandler implements Handler<HttpServerRequest> {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TikaPostHandler.class);

	private final EventBus eb;

	public TikaPostHandler(Vertx vx) {
		this.eb = vx.eventBus();
	}

	@Override
	public void handle(HttpServerRequest event) {
		BinaryFileDataHandler bfdh = new BinaryFileDataHandler();
		event.dataHandler(bfdh);
		event.endHandler(new FileCompleteHandler(bfdh, eb, event.response()));
	}
}
