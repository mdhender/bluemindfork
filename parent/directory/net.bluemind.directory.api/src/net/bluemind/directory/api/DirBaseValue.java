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
package net.bluemind.directory.api;

import java.util.Collection;
import java.util.Collections;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Email;

/**
 * Base class of all directory related entities ({@link DirEntry})
 */
@BMApi(version = "3")
public class DirBaseValue {

	/*
	 * The {@link OrgUnit} of this {@link DirEntry}
	 */
	public String orgUnitUid;

	/**
	 * The {@link DirEntry}'s emails
	 */
	public Collection<Email> emails = Collections.emptyList();

	/**
	 * True when this {@link DirEntry} must not appear in visible views of other
	 * {@link net.bluemind.user.api.User}s
	 */
	public boolean hidden;

	/**
	 * True if the {@link DirEntry} is suspended. A suspended {@link DirEntry}
	 * cannot sign into the system but its data is still in the system.
	 */
	public boolean archived;

	/**
	 * True for some internal {@link DirEntry}, used for specific internal tasks.
	 */
	public boolean system;

	/**
	 * Defines on which {@link net.bluemind.server.api.Server} the data owned by
	 * this {@link DirEntry} are.
	 */
	public String dataLocation;

	/**
	 * @return The default {@link net.bluemind.core.api.Email} for the
	 *         {@link net.bluemind.user.api.User}
	 */
	public Email defaultEmail() {
		if (emails == null) {
			return null;
		}

		Email ret = null;
		for (Email mail : emails) {
			if (mail.isDefault) {
				ret = mail;
				break;
			}
		}
		return ret;
	}

	/**
	 * Fetch the default {@link net.bluemind.core.api.Email} address associated to
	 * this {@link DirEntry}, or null, if no default is present
	 * 
	 * @return the {@link DirEntry}'s default email
	 */
	public String defaultEmailAddress() {
		Email defaultEmail = defaultEmail();
		if (defaultEmail != null) {
			return defaultEmail.address;
		} else {
			return null;
		}
	}

	/**
	 * Fetch the default {@link net.bluemind.core.api.Email} address associated to
	 * this {@link DirEntry}, or null, if no default is present. If the address is
	 * configured as all-alias, the address is returned with the domain's extension.
	 * 
	 * @return the {@link DirEntry}'s default {@link net.bluemind.core.api.Email}
	 */
	public String defaultEmailAddress(String domainDefaultAlias) {
		Email defaultEmail = defaultEmail();
		if (defaultEmail != null) {
			return defaultEmail.allAliases ? defaultEmail.localPart() + "@" + domainDefaultAlias : defaultEmail.address;
		} else {
			return null;
		}
	}

}
