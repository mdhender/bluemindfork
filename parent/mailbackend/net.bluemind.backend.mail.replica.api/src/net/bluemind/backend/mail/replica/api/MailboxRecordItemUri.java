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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemUri;

@BMApi(version = "3")
public class MailboxRecordItemUri extends ItemUri {

	public String bodyGuid;
	public long imapUid;
	public String owner;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((bodyGuid == null) ? 0 : bodyGuid.hashCode());
		result = prime * result + (int) (imapUid ^ (imapUid >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailboxRecordItemUri other = (MailboxRecordItemUri) obj;
		if (bodyGuid == null) {
			if (other.bodyGuid != null)
				return false;
		} else if (!bodyGuid.equals(other.bodyGuid))
			return false;
		if (imapUid != other.imapUid)
			return false;
		return true;
	}

}
