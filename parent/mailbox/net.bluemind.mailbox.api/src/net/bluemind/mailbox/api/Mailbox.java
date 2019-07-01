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
package net.bluemind.mailbox.api;

import java.util.Collection;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Email;
import net.bluemind.server.api.Server;

@BMApi(version = "3")
public final class Mailbox {

	@BMApi(version = "3")
	public static enum Type {
		/**
		 * A user mailbox
		 */
		user("user.", "user/", false),

		/**
		 * A share mail folder
		 */
		mailshare("", "", true),

		/**
		 * A group mailbox (for now, only deliver to users in group)
		 */
		group("", "", true),

		/**
		 * A resource mailbox (deliver ics)
		 */
		resource("", "", true);

		/**
		 * <code>user.</code> or empty string
		 */
		public final String nsPrefix;

		/**
		 * <code>user/</code> or empty string
		 */
		public final String cyrAdmPrefix;
		public final boolean sharedNs;

		private Type(String nsPrefix, String cyrAdmPrefix, boolean shared) {
			this.nsPrefix = nsPrefix;
			this.cyrAdmPrefix = cyrAdmPrefix;
			this.sharedNs = shared;
		}
	}

	@BMApi(version = "3")
	public static enum Routing {
		internal(true), external(false), none(true);

		private final boolean managed;

		Routing(boolean managed) {
			this.managed = managed;
		}

		public boolean managed() {
			return managed;
		}
	};

	public String name;
	public boolean system;
	public boolean hidden;
	public boolean archived;
	public Type type;

	public Routing routing = Routing.none;

	public Collection<Email> emails;

	// serverUid
	/**
	 * {@link Server (bm/imap tagged) }
	 */
	public String dataLocation;

	/**
	 * mail quota in KiB
	 */
	public Integer quota;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (archived ? 1231 : 1237);
		result = prime * result + ((dataLocation == null) ? 0 : dataLocation.hashCode());
		result = prime * result + ((emails == null) ? 0 : emails.hashCode());
		result = prime * result + (hidden ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((quota == null) ? 0 : quota.hashCode());
		result = prime * result + ((routing == null) ? 0 : routing.hashCode());
		result = prime * result + (system ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mailbox other = (Mailbox) obj;
		if (archived != other.archived)
			return false;
		if (dataLocation == null) {
			if (other.dataLocation != null)
				return false;
		} else if (!dataLocation.equals(other.dataLocation))
			return false;
		if (emails == null) {
			if (other.emails != null)
				return false;
		} else if (!emails.equals(other.emails))
			return false;
		if (hidden != other.hidden)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (quota == null) {
			if (other.quota != null)
				return false;
		} else if (!quota.equals(other.quota))
			return false;
		if (routing != other.routing)
			return false;
		if (system != other.system)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
