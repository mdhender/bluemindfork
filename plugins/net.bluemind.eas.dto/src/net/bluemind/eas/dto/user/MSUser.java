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
package net.bluemind.eas.dto.user;

import java.util.Set;

public class MSUser {

	private final String loginAtDomain;
	private final String sid;
	private final String lang;
	private final String defaultEmail;
	private final Set<String> emails;
	private final String uid;
	private final String displayName;
	private final String domain;
	private boolean hasMailbox;
	private final String tz;
	private final String dataLocation;

	public MSUser(String uid, String displayName, String login, String pass, String lang, String tz, boolean hasMailbox,
			String defaultEmail, Set<String> allEmails, String dataLocation) {
		this.uid = uid;
		this.displayName = displayName;
		this.loginAtDomain = login;
		int idx = loginAtDomain.indexOf('@');
		this.domain = loginAtDomain.substring(idx + 1);
		this.sid = pass;
		this.lang = lang;
		this.hasMailbox = hasMailbox;
		this.defaultEmail = defaultEmail;
		this.emails = allEmails;
		this.tz = tz;
		this.dataLocation = dataLocation;
	}

	public String getDataLocation() {
		return dataLocation;
	}

	/**
	 * v3: user.id, v3.5: user uid
	 * 
	 * @return
	 */
	public String getUid() {
		return uid;
	}

	public String getTimeZone() {
		return tz;
	}

	public String getLoginAtDomain() {
		return loginAtDomain;
	}

	public String getSid() {
		return sid;
	}

	public String getLang() {
		return lang;
	}

	public String getDefaultEmail() {
		return defaultEmail;
	}

	public Set<String> getEmails() {
		return emails;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDomain() {
		return domain;
	}

	public boolean hasMailbox() {
		return hasMailbox;
	}

}
