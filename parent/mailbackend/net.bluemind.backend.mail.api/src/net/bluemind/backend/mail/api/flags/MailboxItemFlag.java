/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.mail.api.flags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.BMApi;

/**
 * {@link MailboxItem} flag
 */
@BMApi(version = "3")
public class MailboxItemFlag {

	/**
	 * Flag value (\Seen for example)
	 */
	public String flag;

	/**
	 * numerical 'internal' representation
	 */
	@JsonIgnore
	public int value;

	public MailboxItemFlag() {
	}

	public MailboxItemFlag(String flag) {
		this.flag = flag;
		this.value = 0;
	}

	MailboxItemFlag(String flag, int value) {
		this.flag = flag;
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return flag;
	}

	@JsonCreator
	public static MailboxItemFlag of(@JsonProperty("flag") String value,
			@JsonProperty("value") @SuppressWarnings("unused") int unused) {
		return WellKnownFlags.resolve(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((flag == null) ? 0 : flag.hashCode());
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
		MailboxItemFlag other = (MailboxItemFlag) obj;
		if (flag == null) {
			if (other.flag != null) {
				return false;
			}
		} else if (!flag.equals(other.flag)) {
			return false;
		}
		return true;
	}

	@BMApi(version = "3")
	public enum System {
		Answered(new MailboxItemFlag("\\Answered", 1 << 0)),

		Flagged(new MailboxItemFlag("\\Flagged", 1 << 1)),

		Deleted(new MailboxItemFlag("\\Deleted", 1 << 2)),

		Draft(new MailboxItemFlag("\\Draft", 1 << 3)),

		Seen(new MailboxItemFlag("\\Seen", 1 << 4));

		private final MailboxItemFlag mif;

		private System(MailboxItemFlag f) {
			this.mif = f;
		}

		public MailboxItemFlag value() {
			return mif;
		}
	}

}