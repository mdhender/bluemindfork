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
package net.bluemind.directory.api;

import net.bluemind.core.api.BMApi;

/**
 * Base data structure of all directory related entities
 */
@BMApi(version = "3")
public class BaseDirEntry {

	public String displayName;
	/**
	 * Unique entry uid
	 */
	public String entryUid;
	/**
	 * The {@link DirEntry}'s {@link BaseDirEntry.AccountType} type
	 */
	public AccountType accountType;
	/**
	 * The {@link BaseDirEntry.Kind} of the {@link DirEntry}
	 */
	public Kind kind = Kind.DOMAIN;

	/**
	 * The type of an account.<br>
	 * FULL represents an entity with all available applications (messaging,
	 * contact, agenda etc.)<br>
	 * SIMPLE represents an entity with limited access (no sharing) to the messaging
	 * application FULL_AND_VISIO represents an entity having advanced features to
	 * the video conferencing functionalities
	 */
	@BMApi(version = "3")
	public static enum AccountType {
		FULL, SIMPLE, FULL_AND_VISIO;
	}

	/**
	 * The kind of object the entry represents.
	 */
	@BMApi(version = "3")
	public enum Kind {
		USER(true),

		GROUP(true),

		RESOURCE(true),

		MAILSHARE(true),

		CALENDAR, ADDRESSBOOK, DOMAIN, ORG_UNIT, EXTERNALUSER;

		private final boolean mbox;

		private Kind() {
			this(false);
		}

		private Kind(boolean mbox) {
			this.mbox = mbox;
		}

		public boolean hasMailbox() {
			return true;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryUid == null) ? 0 : entryUid.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
		BaseDirEntry other = (BaseDirEntry) obj;
		if (entryUid == null) {
			if (other.entryUid != null)
				return false;
		} else if (!entryUid.equals(other.entryUid))
			return false;
		if (kind != other.kind)
			return false;
		return true;
	}

}
