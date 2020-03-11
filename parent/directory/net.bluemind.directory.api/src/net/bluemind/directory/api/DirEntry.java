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

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.WriteOnly;

/**
 * Data structure of directory related entities
 */
@BMApi(version = "3")
public class DirEntry extends BaseDirEntry {
	/**
	 * Path of the {@link DirEntry} in the format<br>
	 * <b>domainUid/kind/entryUid</b>
	 */
	public String path;
	/**
	 * Email address
	 */
	public String email;
	/**
	 * True, if this {@link DirEntry} is hidden from all user views
	 */
	public boolean hidden;
	/**
	 * True, if this is a system-internal account
	 */
	public boolean system;
	/**
	 * True, if this is a system-internal account
	 */
	public boolean archived;
	/**
	 * List of emails associated to this {@link DirEntry}
	 */
	@WriteOnly
	public List<Email> emails;
	/**
	 * Optional unique if of the organizational unit this {@link DirEntry} belongs
	 * to
	 */
	public String orgUnitUid;
	/**
	 * Optional unique if of the organizational unit this {@link DirEntry} belongs
	 * to
	 */
	public OrgUnitPath orgUnitPath;
	/**
	 * The unique id of the {@link net.bluemind.server.api.Server} this
	 * {@link DirEntry} belongs to
	 */
	public String dataLocation;

	// FIXME remove path parameter
	public static DirEntry create(String orgUnitUid, String path, Kind kind, String entryUid, String displayName,
			String email, boolean hidden, boolean system, boolean archived) {
		DirEntry ret = new DirEntry();
		ret.orgUnitUid = orgUnitUid;
		ret.path = path;
		ret.kind = kind;
		ret.entryUid = entryUid;
		ret.displayName = displayName;
		ret.email = email;
		ret.hidden = hidden;
		ret.system = system;
		ret.archived = archived;
		return ret;
	}

	public static DirEntry create(String orgUnitUid, String path, Kind kind, String entryUid, String displayName,
			String email, boolean hidden, boolean system, boolean archived, String dataLocation) {
		DirEntry ret = DirEntry.create(orgUnitUid, path, kind, entryUid, displayName, email, hidden, system, archived);
		ret.dataLocation = dataLocation;
		return ret;
	}

	public static DirEntry create(String orgUnitUid, String path, Kind kind, String entryUid, String displayName,
			String email, boolean hidden, boolean system, boolean archived, String dataLocation,
			AccountType accountType) {
		DirEntry ret = DirEntry.create(orgUnitUid, path, kind, entryUid, displayName, email, hidden, system, archived,
				dataLocation);
		ret.accountType = accountType;
		return ret;
	}

	/**
	 * Adds an array of {@link net.bluemind.core.api.Email}s to this
	 * {@link DirEntry}
	 * 
	 * @param emails array of {@link net.bluemind.core.api.Email}s
	 * @return the {@link DirEntry} itself
	 */
	public DirEntry withEmails(String... emails) {
		boolean d = true;
		ArrayList<Email> ret = new ArrayList<>(emails.length);
		for (String e : emails) {
			Email email = Email.create(e, d);
			d = false;
			ret.add(email);
		}
		this.emails = ret;
		return this;
	}

	/**
	 * Adds a list of {@link net.bluemind.core.api.Email}s to this {@link DirEntry}
	 * 
	 * @param emails list of {@link net.bluemind.core.api.Email}s
	 * @return the {@link DirEntry} itself
	 */
	public DirEntry withEmails(List<Email> emails) {
		this.emails = emails;
		return this;
	}

	@Override
	public String toString() {
		return "DirEntry [kind=" + kind + ", path=" + path + ", displayName=" + displayName + ", entryUid=" + entryUid
				+ ", email=" + email + ", archived = " + archived + ", dataLocation = " + dataLocation + "]";
	}
}
