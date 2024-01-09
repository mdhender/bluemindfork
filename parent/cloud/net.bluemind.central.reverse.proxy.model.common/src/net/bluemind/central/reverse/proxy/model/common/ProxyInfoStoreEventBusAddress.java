package net.bluemind.central.reverse.proxy.model.common;

import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.DeliveryOptions;

public class ProxyInfoStoreEventBusAddress {
	public static final long TIME_PROCES_WARN = TimeUnit.SECONDS.toMillis(30);
	public static final long TIME_MANAGE_WARN = TimeUnit.SECONDS.toMillis(10);

	public static final String ADDRESS = "proxy-info-address";

	public static final String HEADER_ACTION = "action";
	public static final String HEADER_TS = "ts";

	public static final String ADD_DIR_NAME = "addDir";
	public static final String ADD_DOMAIN_NAME = "addDomain";
	public static final String ADD_INSTALLATION_NAME = "addInstallation";
	public static final String IP_NAME = "ip";
	public static final String ALL_IPS_NAME = "allIps";
	public static final String ANY_IP_NAME = "anyIp";

	public static final DeliveryOptions ADD_DIR = getDeliveryOptions().addHeader(HEADER_ACTION, ADD_DIR_NAME);
	public static final DeliveryOptions ADD_DOMAIN = getDeliveryOptions().addHeader(HEADER_ACTION, ADD_DOMAIN_NAME);
	public static final DeliveryOptions ADD_INSTALLATION = getDeliveryOptions().addHeader(HEADER_ACTION,
			ADD_INSTALLATION_NAME);
	public static final DeliveryOptions IP = getDeliveryOptions().addHeader(HEADER_ACTION, IP_NAME);
	public static final DeliveryOptions ALL_IPS = getDeliveryOptions().addHeader(HEADER_ACTION, ALL_IPS_NAME);
	public static final DeliveryOptions ANY_IP = getDeliveryOptions().addHeader(HEADER_ACTION, ANY_IP_NAME);

	private ProxyInfoStoreEventBusAddress() {
	}

	private static DeliveryOptions getDeliveryOptions() {
		return new DeliveryOptions().setSendTimeout(TimeUnit.MINUTES.toMillis(2)).addHeader(HEADER_TS,
				Long.toString(System.currentTimeMillis()));
	}
}
