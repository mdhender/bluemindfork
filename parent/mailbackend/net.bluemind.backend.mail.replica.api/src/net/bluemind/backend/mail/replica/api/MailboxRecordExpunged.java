/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
public class MailboxRecordExpunged {

	public Integer containerId;
	public Integer subtreeId;
	public Long itemId;
	public Long imapUid;
	public Date created;

	public Integer containerId() {
		return containerId;
	}

	public Long imapUid() {
		return imapUid;
	}

	@Override
	public String toString() {
		return super.toString() + "[created: " + created + ", itemId: " + itemId + ", imapUid: " + imapUid
				+ ", containerId: " + containerId + ", subtreeId: " + subtreeId + "]";
	}

}
