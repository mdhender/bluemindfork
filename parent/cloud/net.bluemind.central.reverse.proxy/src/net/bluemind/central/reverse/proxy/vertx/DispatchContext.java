package net.bluemind.central.reverse.proxy.vertx;

import io.vertx.core.net.SocketAddress;

public class DispatchContext<T> {

	public SocketAddress address;
	public T info;

	private DispatchContext(SocketAddress address, T info) {
		this.address = address;
		this.info = info;
	}

	public static <T> DispatchContext<T> create(SocketAddress address, T info) {
		return new DispatchContext<>(address, info);
	}

}
