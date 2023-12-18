package net.bluemind.central.reverse.proxy.model.common;

import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.DeliveryOptions;

public class PostfixMapsStoreEventBusAddress {
	public static final long TIME_WARN = TimeUnit.SECONDS.toMillis(10);

	public static final String ADDRESS = "postfix-maps-address";

	public static final String ACTION_HEADER = "action";

	public static final String ADD_INSTALLATION_NAME = "addInstallation";
	public static final String ADD_DIR_NAME = "addDir";
	public static final String DEL_DIR_NAME = "delDir";
	public static final String ADD_DOMAIN_NAME = "addDomain";
	public static final String UPDATE_DOMAIN_SETTINGS_NAME = "updateDomainSettings";
	public static final String DEL_DOMAIN_NAME = "delDomain";
	public static final String MANAGE_MEMBER_NAME = "manageMember";
	public static final String ALIAS_TO_MAILBOX = "aliasToMailbox";
	public static final String MAILBOX_EXISTS = "mailboxExists";
	public static final String MAILBOX_DOMAIN_MANAGED = "mailboxDomainManaged";
	public static final String MAILBOX_STORE = "mailboxStore";
	public static final String SRS_RECIPIENT = "srsRecipient";

	public static final DeliveryOptions ADD_INSTALLATION = getDeliveryOptions().addHeader(ACTION_HEADER,
			ADD_INSTALLATION_NAME);
	public static final DeliveryOptions ADD_DIR = getDeliveryOptions().addHeader(ACTION_HEADER, ADD_DIR_NAME);
	public static final DeliveryOptions DEL_DIR = getDeliveryOptions().addHeader(ACTION_HEADER, DEL_DIR_NAME);
	public static final DeliveryOptions ADD_DOMAIN = getDeliveryOptions().addHeader(ACTION_HEADER, ADD_DOMAIN_NAME);
	public static final DeliveryOptions UPDATE_DOMAIN_SETTINGS = getDeliveryOptions().addHeader(ACTION_HEADER,
			UPDATE_DOMAIN_SETTINGS_NAME);
	public static final DeliveryOptions MANAGE_MEMBER = getDeliveryOptions().addHeader(ACTION_HEADER,
			MANAGE_MEMBER_NAME);
	public static final DeliveryOptions GET_ALIAS_TO_DOMAIN = getDeliveryOptions().addHeader(ACTION_HEADER,
			ALIAS_TO_MAILBOX);
	public static final DeliveryOptions GET_MAILBOX_EXISTS = getDeliveryOptions().addHeader(ACTION_HEADER,
			MAILBOX_EXISTS);
	public static final DeliveryOptions GET_MAILBOX_DOMAIN_MANAGED = getDeliveryOptions().addHeader(ACTION_HEADER,
			MAILBOX_DOMAIN_MANAGED);
	public static final DeliveryOptions GET_MAILBOX_STORE = getDeliveryOptions().addHeader(ACTION_HEADER,
			MAILBOX_STORE);
	public static final DeliveryOptions GET_SRS_RECIPIENT = getDeliveryOptions().addHeader(ACTION_HEADER,
			SRS_RECIPIENT);

	private PostfixMapsStoreEventBusAddress() {
	}

	private static DeliveryOptions getDeliveryOptions() {
		return new DeliveryOptions().setSendTimeout(TimeUnit.MINUTES.toMillis(2));
	}
}
