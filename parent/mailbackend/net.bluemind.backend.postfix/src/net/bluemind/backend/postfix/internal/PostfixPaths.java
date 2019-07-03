/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.postfix.internal;

public class PostfixPaths {

	public static final String MAIN_CF = "/etc/postfix/main.cf";

	public static final String MASTER_CF = "/etc/postfix/master.cf";

	public static final String SASL_SMTPD_CONF = "/etc/postfix/sasl/smtpd.conf";

	public static final String DB_MAP_SUFFIX = ".db";

	public static final String VIRTUAL_DOMAINS = "/etc/postfix/virtual_domains";
	public static final String DB_VIRTUAL_DOMAINS = VIRTUAL_DOMAINS + DB_MAP_SUFFIX;
	public static final String FLAT_VIRTUAL_DOMAINS = "/etc/postfix/virtual_domains-flat";

	public static final String VIRTUAL_MAILBOX = "/etc/postfix/virtual_mailbox";
	public static final String DB_VIRTUAL_MAILBOX = VIRTUAL_MAILBOX + DB_MAP_SUFFIX;
	public static final String FLAT_VIRTUAL_MAILBOX = "/etc/postfix/virtual_mailbox-flat";

	public static final String VIRTUAL_ALIAS = "/etc/postfix/virtual_alias";
	public static final String DB_VIRTUAL_ALIAS = VIRTUAL_ALIAS + DB_MAP_SUFFIX;
	public static final String FLAT_VIRTUAL_ALIAS = "/etc/postfix/virtual_alias-flat";

	public static final String TRANSPORT = "/etc/postfix/transport";
	public static final String DB_TRANSPORT = TRANSPORT + DB_MAP_SUFFIX;
	public static final String FLAT_TRANSPORT = "/etc/postfix/transport-flat";

	public static final String RELAY_TRANSPORT = "/etc/postfix/master_relay_transport";
	public static final String DB_RELAY_TRANSPORT = RELAY_TRANSPORT + DB_MAP_SUFFIX;
	public static final String FLAT_RELAY_TRANSPORT = "/etc/postfix/master_relay_transport-flat";

	public static final String RELAY_PASSWORD = "/etc/postfix/relay_passwd";// NOSONAR

}