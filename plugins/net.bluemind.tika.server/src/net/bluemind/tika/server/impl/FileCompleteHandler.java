package net.bluemind.tika.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class FileCompleteHandler implements Handler<Void> {

	private static final Logger logger = LoggerFactory.getLogger(FileCompleteHandler.class);
	private final BinaryFileDataHandler bfdh;
	private final EventBus eb;
	private final HttpServerResponse r;

	public FileCompleteHandler(BinaryFileDataHandler bfdh, EventBus eventBus, HttpServerResponse r) {
		this.bfdh = bfdh;
		this.eb = eventBus;
		this.r = r;
	}

	@Override
	public void handle(Void event) {
		logger.info("File complete {}", bfdh.getFilePath());
		final String hash = bfdh.flushAndHash();
		final long start = System.currentTimeMillis();
		JsonObject toExtract = new JsonObject().put("hash", hash).put("path", bfdh.getFilePath());
		eb.request("tika.extract", toExtract, new DeliveryOptions().setSendTimeout(5000),
				(AsyncResult<Message<String>> evt) -> {
					bfdh.cleanup();
					String theText = "";
					MultiMap headers = r.headers();
					headers.add("Content-Type", "text/plain; charset=utf-8");
					headers.add("X-BM-TikaHash", hash);

					if (evt.failed()) {
						logger.warn("tika.extract failed: {}", evt.cause().getMessage());
					} else {
						theText = evt.result().body();
					}
					r.end(theText);
					logger.info("Extracted {} characters in {}ms.", theText.length(),
							System.currentTimeMillis() - start);
				});

	}
}
