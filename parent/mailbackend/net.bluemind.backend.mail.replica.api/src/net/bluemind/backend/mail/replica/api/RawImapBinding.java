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

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class RawImapBinding {

	public long itemId;
	public long imapUid;

	@Override
	public String toString() {
		return "RawBinding{id: " + itemId + ", uid: " + imapUid + "}";
	}

	public static RawImapBinding of(long imapUid, long internalId) {
		RawImapBinding raw = new RawImapBinding();
		raw.imapUid = imapUid;
		raw.itemId = internalId;
		return raw;
	}
}
