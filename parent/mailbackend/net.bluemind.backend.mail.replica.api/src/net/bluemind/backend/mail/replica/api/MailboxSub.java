/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.api;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailboxSub {

	public String userId;
	public String mboxName;

	public MailboxSub() {
	}

	public MailboxSub(String uid, String mbox) {
		this.userId = uid;
		this.mboxName = mbox;
	}

	public static MailboxSub of(JsonObject js) {
		return new MailboxSub(js.getString("USERID"), js.getString("MBOXNAME"));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mboxName == null) ? 0 : mboxName.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		MailboxSub other = (MailboxSub) obj;
		if (mboxName == null) {
			if (other.mboxName != null)
				return false;
		} else if (!mboxName.equals(other.mboxName))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
