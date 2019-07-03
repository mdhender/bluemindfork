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
	public long modSeq;
	public String itemUid;

	public static Creator<RecordID> CREATOR = con -> new RecordID();
	public static EntityPopulator<RecordID> POPULATOR = (rs, index, value) -> {
		value.imapUid = rs.getLong(index++);
		value.modSeq = rs.getLong(index++);
		value.itemUid = rs.getString(index++);
		return index;
	};

	private RecordID() {
	}

	public RecordID(long imapUid, long modSeq) {
		this.imapUid = imapUid;
		this.modSeq = modSeq;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (imapUid ^ (imapUid >>> 32));
		result = prime * result + (int) (modSeq ^ (modSeq >>> 32));
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
		if (imapUid != other.imapUid)
			return false;
		if (modSeq != other.modSeq)
			return false;
		return true;
	}

}
