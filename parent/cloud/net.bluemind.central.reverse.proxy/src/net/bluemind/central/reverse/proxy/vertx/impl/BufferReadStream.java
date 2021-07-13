package net.bluemind.central.reverse.proxy.vertx.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class BufferReadStream implements ReadStream<Buffer> {

	private final Logger logger = LoggerFactory.getLogger(BufferReadStream.class);

	private Buffer data;
	private boolean finished;
	private Handler<Void> endHandler;
	private boolean running = false;
	private Handler<Buffer> dataHandler;

	public BufferReadStream(Buffer data) {
		this.data = data;
	}

	@Override
	public BufferReadStream handler(Handler<Buffer> handler) {
		this.dataHandler = handler;
		read();
		return this;
	}

	private void read() {
		if (!running) {
			return;
		}

		logger.info("reading: {}", data);
		if (data.length() > 0) {
			dataHandler.handle(data);
		}
		logger.info("dataHandler: {}", dataHandler);
		ended();
	}

	private void ended() {
		logger.info("ended: {}", finished);
		if (finished) {
			return;

		}

		finished = true;
		if (endHandler != null) {
			endHandler.handle(null);
		}
	}

	@Override
	public BufferReadStream pause() {
		running = false;
		return null;
	}

	@Override
	public BufferReadStream resume() {
		running = true;
		if (!finished) {
			read();
		}
		return this;
	}

	@Override
	public BufferReadStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public BufferReadStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}

}
