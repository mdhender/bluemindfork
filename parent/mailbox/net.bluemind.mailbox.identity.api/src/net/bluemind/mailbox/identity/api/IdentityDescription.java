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
package net.bluemind.mailbox.identity.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.Mailbox;

@BMApi(version = "3")
public class IdentityDescription {

	/**
	 * {@link Mailbox} uid
	 */
	public String mbox;

	/**
	 * identity id (work, personal, free string)
	 */
	public String id;

	/**
	 * the email that will be used to send the e-mail when using this identity.
	 */
	public String email;

	/**
	 * the identity name that will be used in the from header of the mail alongside
	 * with {@link #email}
	 */
	public String name;

	public Boolean isDefault;

	public String displayname;

	public String signature;

	/** @see {@link Mailbox#name} */
	public String mboxName;
}
