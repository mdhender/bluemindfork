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
package net.bluemind.user.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirBaseValue;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;

/**
 * User represents a specific user, with a login and a password and a list of
 * emails. Informations like firstname, lastname and phones are store in a
 * {@link VCard}.
 * 
 * Users are part of a domain (and only one).
 * 
 * They are stored as items in a container users_[domainUid].
 */
@BMApi(version = "3")
public final class User extends DirBaseValue {

	/**
	 * The login of the user. The login at (@) domain name is used to sign-into the
	 * system.
	 */
	@Required
	public String login;

	/**
	 * The password is set to the clear-text version to update the password. It is
	 * never fetched and only a hash of it is stored in the database.
	 */
	public String password;

	/**
	 * Last password update date.
	 */
	public Date passwordLastChange;

	/**
	 * Password must be changed
	 */
	public boolean passwordMustChange = false;

	/**
	 * Password never expire
	 */
	public boolean passwordNeverExpires = false;

	/**
	 * Contact informations for the user (firstname, lastname, phones, etc)
	 */
	public VCard contactInfos; // "user_" + uid

	/**
	 * Defines how mail routing will be done for this user
	 */
	public Mailbox.Routing routing = Routing.none;

	public AccountType accountType = AccountType.FULL;

	/**
	 * {@link Mailbox#quota}
	 */
	public Integer quota;

	/**
	 * Order the "clients" (webapp, calendar, contacts) to clear their local data if
	 * their data was initialized with a different mailboxCopyGuid.
	 */
	public String mailboxCopyGuid;

	/**
	 * Custom properties
	 */
	public Map<String, String> properties = new HashMap<>();

	@Override
	public String toString() {
		return "User [login=" + login + ", archived=" + archived + ", passwordLastChange=" + passwordLastChange
				+ ", passwordMustChange=" + passwordMustChange + ", system=" + system + ", hidden=" + hidden
				+ ", routing=" + routing + ", dataLocation=" + dataLocation + "]";
	}

	public boolean fullAccount() {
		return accountType == AccountType.FULL || accountType == AccountType.FULL_AND_VISIO;
	}
}
