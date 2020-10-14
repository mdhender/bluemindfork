/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.persistence;

import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;

public class RecordID {

	public long imapUid;
	public long itemId;

	public static final Creator<RecordID> CREATOR = con -> new RecordID();
	public static final EntityPopulator<RecordID> POPULATOR = (rs, index, value) -> {
		value.imapUid = rs.getLong(index++);
		value.itemId = rs.getLong(index++);
		return index;
	};

	private RecordID() {
	}

	public RecordID(long imapUid) {
		this.imapUid = imapUid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (imapUid ^ (imapUid >>> 32));
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
		RecordID other = (RecordID) obj;
		return imapUid == other.imapUid;
	}

	@Override
	public String toString() {
		return "RecordID{imap: " + imapUid + ", item: " + itemId + "}";
	}

}