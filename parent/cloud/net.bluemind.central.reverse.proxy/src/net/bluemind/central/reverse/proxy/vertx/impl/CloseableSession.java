package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Promise;
import io.vertx.core.net.SocketAddress;

public class CloseableSession {
	private final Logger logger = LoggerFactory.getLogger(CloseableSession.class);

	private final String id;
	private final SocketAddress address;
	private final Promise<Void> closePromise;
	private final Promise<Void> endPromise;

	public CloseableSession(SocketAddress address) {
		this.id = UUID.randomUUID().toString();
		this.address = address;
		this.closePromise = Promise.promise();
		this.endPromise = Promise.promise();
	}

	public String id() {
		return id;
	}

	public SocketAddress address() {
		return address;
	}

	public void close() {
		closePromise.tryComplete();
	}

	public void end() {
		endPromise.tryComplete();
	}

	public void onClose(Runnable action) {
		closePromise.future().onComplete(ar -> action.run());
	}

	public void onEnd(Runnable action) {
		endPromise.future().onComplete(ar -> action.run());
	}

}
