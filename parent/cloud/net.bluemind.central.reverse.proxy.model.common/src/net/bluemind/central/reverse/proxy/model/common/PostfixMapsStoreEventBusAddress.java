package net.bluemind.central.reverse.proxy.model.common;

import io.vertx.core.eventbus.DeliveryOptions;

public class PostfixMapsStoreEventBusAddress {
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

	public static final DeliveryOptions ADD_INSTALLATION = new DeliveryOptions().addHeader(ACTION_HEADER,
			ADD_INSTALLATION_NAME);
	public static final DeliveryOptions ADD_DIR = new DeliveryOptions().addHeader(ACTION_HEADER, ADD_DIR_NAME);
	public static final DeliveryOptions DEL_DIR = new DeliveryOptions().addHeader(ACTION_HEADER, DEL_DIR_NAME);
	public static final DeliveryOptions ADD_DOMAIN = new DeliveryOptions().addHeader(ACTION_HEADER, ADD_DOMAIN_NAME);
	public static final DeliveryOptions UPDATE_DOMAIN_SETTINGS = new DeliveryOptions().addHeader(ACTION_HEADER,
			UPDATE_DOMAIN_SETTINGS_NAME);
	public static final DeliveryOptions MANAGE_MEMBER = new DeliveryOptions().addHeader(ACTION_HEADER,
			MANAGE_MEMBER_NAME);
	public static final DeliveryOptions GET_ALIAS_TO_DOMAIN = new DeliveryOptions().addHeader(ACTION_HEADER,
			ALIAS_TO_MAILBOX);
	public static final DeliveryOptions GET_MAILBOX_EXISTS = new DeliveryOptions().addHeader(ACTION_HEADER,
			MAILBOX_EXISTS);
	public static final DeliveryOptions GET_MAILBOX_DOMAIN_MANAGED = new DeliveryOptions().addHeader(ACTION_HEADER,
			MAILBOX_DOMAIN_MANAGED);
	public static final DeliveryOptions GET_MAILBOX_STORE = new DeliveryOptions().addHeader(ACTION_HEADER,
			MAILBOX_STORE);

	private PostfixMapsStoreEventBusAddress() {
	}
}
