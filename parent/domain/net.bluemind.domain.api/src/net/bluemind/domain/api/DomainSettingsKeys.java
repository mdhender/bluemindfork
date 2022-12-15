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

import jakarta.validation.constraints.NotNull;

public enum DomainSettingsKeys {
	@NotNull
	lang, //
	@NotNull
	date, //
	@NotNull
	timeformat, //
	@NotNull
	timezone, //
	@NotNull
	im_public_auth, //
	mailbox_max_user_quota, //
	mailbox_default_user_quota, //
	mailbox_max_publicfolder_quota, //
	mailbox_default_publicfolder_quota, //
	@NotNull
	mail_routing_relay, //
	@NotNull
	mail_forward_unknown_to_relay, //
	domain_max_users, //
	domain_max_basic_account, //
	password_lifetime, //
	domain_max_fullvisio_accounts, //
	@NotNull
	cti_implementation, //
	@NotNull
	cti_host, //
	@NotNull
	external_url, //
	@NotNull
	other_urls, //
	@NotNull
	default_domain, //
	@NotNull
	ssl_certif_engine
}
