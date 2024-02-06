package net.bluemind.central.reverse.proxy.model.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.DeliveryOptions;

public class PostfixMapsStoreEventBusAddress {
	public static final long TIME_PROCES_WARN = TimeUnit.SECONDS.toNanos(30);
	public static final long TIME_MANAGE_WARN = TimeUnit.SECONDS.toNanos(10);

	public static final String ADDRESS = "postfix-maps-address";

	public static final String HEADER_ACTION = "action";
	public static final String HEADER_TS = "ts";

	public enum PostfixActionHeader {
		ADD_INSTALLATION("addInstallation"), //
		ADD_DIR("addDir"), //
		DEL_DIR("delDir"), //
		ADD_DOMAIN("addDomain"), //
		UPDATE_DOMAIN_SETTINGS("updateDomainSettings"), //
		DEL_DOMAIN("delDomain"), //
		MANAGE_MEMBER("manageMember"), //
		ALIAS_TO_MAILBOX("aliasToMailbox"), //
		MAILBOX_EXISTS("mailboxExists"), //
		MAILBOX_DOMAIN_MANAGED("mailboxDomainManaged"), //
		MAILBOX_STORE("mailboxStore"), //
		SRS_RECIPIENT("srsRecipient");

		private String value;

		private static final Map<String, PostfixActionHeader> EMAP;

		static {
			Map<String, PostfixActionHeader> valueMap = new HashMap<>();
			for (PostfixActionHeader ahdr : PostfixActionHeader.values()) {
				valueMap.put(ahdr.getValue(), ahdr);
			}
			EMAP = Collections.unmodifiableMap(valueMap);
		}

		PostfixActionHeader(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static PostfixActionHeader fromString(String value) {
			return EMAP.get(value);
		}

		public DeliveryOptions getDeliveryOptions() {
			return new DeliveryOptions().setSendTimeout(TimeUnit.MINUTES.toMillis(2))
					.addHeader(HEADER_TS, Long.toString(System.nanoTime())).addHeader(HEADER_ACTION, this.getValue());
		}
	}

	private PostfixMapsStoreEventBusAddress() {
	}

}
