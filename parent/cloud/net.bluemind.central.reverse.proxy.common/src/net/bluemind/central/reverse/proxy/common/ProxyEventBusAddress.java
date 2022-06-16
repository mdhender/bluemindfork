package net.bluemind.central.reverse.proxy.common;

import io.vertx.core.eventbus.DeliveryOptions;

public class ProxyEventBusAddress {

	private ProxyEventBusAddress() {

	}

	public static final String ADDRESS = "proxy-address";

	public static final String ACTION_HEADER = "action";

	public static final String STREAM_READY_NAME = "stream-ready";
	public static final String MODEL_READY_NAME = "model-ready";
	public static final String INSTALLATION_IP_CHANGE_NAME = "ip-change";

	public static final DeliveryOptions STREAM_READY = new DeliveryOptions().addHeader(ACTION_HEADER,
			STREAM_READY_NAME);
	public static final DeliveryOptions MODEL_READY = new DeliveryOptions().addHeader(ACTION_HEADER, MODEL_READY_NAME);
	public static final DeliveryOptions INSTALLATION_IP_CHANGE = new DeliveryOptions().addHeader(ACTION_HEADER,
			INSTALLATION_IP_CHANGE_NAME);

}
