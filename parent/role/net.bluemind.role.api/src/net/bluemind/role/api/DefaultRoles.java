/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.role.api;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Declaration of default user roles.
 */
public class DefaultRoles {

	/**
	 * Default roles of a simple user accounts.
	 */
	public static final Set<String> SIMPLE_USER_DEFAULT_ROLES = ImmutableSet.<String>builder().add(
			BasicRoles.ROLE_MAIL_FORWARDING, //
			BasicRoles.SELF_CHANGE_PASSWORD, //
			BasicRoles.ROLE_SELF_CHANGE_SETTINGS, //
			BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES, //
			BasicRoles.ROLE_SELF_CHANGE_MAILBOX_FILTER, //
			BasicRoles.ROLE_READ_DOMAIN_FILTER, //
			BasicRoles.ROLE_WEBMAIL).build();

	/**
	 * Additional FULL user account roles (includes simple user account roles).
	 */
	public static final Set<String> USER_DEFAULT_ROLES = ImmutableSet.<String>builder().add(BasicRoles.ROLE_CALENDAR, //
			BasicRoles.ROLE_EAS, //
			BasicRoles.ROLE_DAV, //
			BasicRoles.ROLE_TBIRD, //
			BasicRoles.ROLE_OUTLOOK).addAll(SIMPLE_USER_DEFAULT_ROLES).build();

	public static final Set<String> USER_PASSWORD_EXPIRED = ImmutableSet.<String>builder()
			.add(BasicRoles.SELF_CHANGE_PASSWORD).build();

	/**
	 * Default administrator roles.
	 */
	public static final Set<String> ADMIN_DEFAULT_ROLES = ImmutableSet.<String>builder()
			.add(BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_ADMINCONSOLE).addAll(USER_DEFAULT_ROLES).build();
}
