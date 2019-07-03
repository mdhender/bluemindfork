package net.bluemind.tika.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

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
		JsonObject toExtract = new JsonObject().putString("hash", hash).putString("path", bfdh.getFilePath());
		eb.sendWithTimeout("tika.extract", toExtract, 5000, new Handler<AsyncResult<Message<String>>>() {

			@Override
			public void handle(AsyncResult<Message<String>> event) {
				bfdh.cleanup();
				String theText = "";
				MultiMap headers = r.headers();
				headers.add("Content-Type", "text/plain; charset=utf-8");
				headers.add("X-BM-TikaHash", hash);

				if (event.failed()) {
					logger.warn("tika.extract failed: {}", event.cause().getMessage());
				} else {
					theText = event.result().body();
				}
				r.end(theText);
				logger.info("Extracted {} characters in {}ms.", theText.length(), System.currentTimeMillis() - start);
			}
		});
	}
}
