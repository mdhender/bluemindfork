package net.bluemind.central.reverse.proxy.model.common;

import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.DeliveryOptions;

public class ProxyInfoStoreEventBusAddress {
	public static final long TIME_WARN = TimeUnit.SECONDS.toMillis(10);

	public static final String ADDRESS = "proxy-info-address";

	public static final String ACTION_HEADER = "action";

	public static final String ADD_DIR_NAME = "addDir";
	public static final String ADD_DOMAIN_NAME = "addDomain";
	public static final String ADD_INSTALLATION_NAME = "addInstallation";
	public static final String IP_NAME = "ip";
	public static final String ALL_IPS_NAME = "allIps";
	public static final String ANY_IP_NAME = "anyIp";

	public static final DeliveryOptions ADD_DIR = getDeliveryOptions().addHeader(ACTION_HEADER, ADD_DIR_NAME);
	public static final DeliveryOptions ADD_DOMAIN = getDeliveryOptions().addHeader(ACTION_HEADER, ADD_DOMAIN_NAME);
	public static final DeliveryOptions ADD_INSTALLATION = getDeliveryOptions().addHeader(ACTION_HEADER,
			ADD_INSTALLATION_NAME);
	public static final DeliveryOptions IP = getDeliveryOptions().addHeader(ACTION_HEADER, IP_NAME);
	public static final DeliveryOptions ALL_IPS = getDeliveryOptions().addHeader(ACTION_HEADER, ALL_IPS_NAME);
	public static final DeliveryOptions ANY_IP = getDeliveryOptions().addHeader(ACTION_HEADER, ANY_IP_NAME);

	private ProxyInfoStoreEventBusAddress() {
	}

	private static DeliveryOptions getDeliveryOptions() {
		return new DeliveryOptions().setSendTimeout(TimeUnit.MINUTES.toMillis(2));
	}
}
