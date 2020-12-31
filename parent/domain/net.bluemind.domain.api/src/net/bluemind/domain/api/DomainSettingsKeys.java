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
package net.bluemind.domain.api;

public enum DomainSettingsKeys {
	lang, //
	timezone, //
	im_public_auth, //
	mailbox_max_user_quota, //
	mailbox_default_user_quota, //
	mailbox_max_publicfolder_quota, //
	mailbox_default_publicfolder_quota, //
	mail_routing_relay, //
	mail_forward_unknown_to_relay, //
	domain_max_users, //
	domain_max_basic_account, //
	password_lifetime
}
