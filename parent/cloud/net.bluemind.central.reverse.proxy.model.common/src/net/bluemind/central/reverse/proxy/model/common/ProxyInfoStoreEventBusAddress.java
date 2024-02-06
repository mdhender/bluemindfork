package net.bluemind.central.reverse.proxy.model.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.DeliveryOptions;

public class ProxyInfoStoreEventBusAddress {
	public static final long TIME_PROCES_WARN = TimeUnit.SECONDS.toNanos(30);
	public static final long TIME_MANAGE_WARN = TimeUnit.SECONDS.toNanos(10);

	public static final String ADDRESS = "proxy-info-address";

	public static final String HEADER_ACTION = "action";
	public static final String HEADER_TS = "ts";

	public enum ActionHeader {
		ADD_DIR("addDir"), //
		ADD_DOMAIN("addDomain"), //
		ADD_INSTALLATION("addInstallation"), //
		IP("ip"), //
		ALL_IPS("allIps"), //
		ANY_IP("anyIp");

		private static final Map<String, ActionHeader> EMAP;

		static {
			Map<String, ActionHeader> valueMap = new HashMap<>();
			for (ActionHeader ahdr : ActionHeader.values()) {
				valueMap.put(ahdr.getValue(), ahdr);
			}
			EMAP = Collections.unmodifiableMap(valueMap);
		}

		private String value;

		ActionHeader(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static ActionHeader fromString(String value) {
			return EMAP.get(value);
		}

		public DeliveryOptions getDeliveryOptions() {
			return new DeliveryOptions().setSendTimeout(TimeUnit.MINUTES.toMillis(2))
					.addHeader(HEADER_TS, Long.toString(System.nanoTime())).addHeader(HEADER_ACTION, this.getValue());
		}
	}

	private ProxyInfoStoreEventBusAddress() {
	}

}
