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
package net.bluemind.backend.mail.api;

import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;

@BMApi(version = "3")
@GwtIncompatible
public class ImportMailboxItemSet {

	@BMApi(version = "3")
	public static class MailboxItemId {
		public long id;

		public static MailboxItemId of(long id) {
			MailboxItemId ret = new MailboxItemId();
			ret.id = id;
			return ret;
		}
	}

	public long mailboxFolderId;

	/**
	 * MailboxItems ids
	 */
	public List<MailboxItemId> ids;

	/**
	 * Expected MailboxItems ids
	 * 
	 * list can be null ortherwise size must be equals to
	 * {@link ImportMailboxItemSet#ids} size
	 */
	public List<MailboxItemId> expectedIds;

	public boolean deleteFromSource;

	public static ImportMailboxItemSet copyIn(long mailboxFolderId, List<MailboxItemId> ids,
			List<MailboxItemId> expectedIds) {
		return ImportMailboxItemSet.of(mailboxFolderId, ids, expectedIds, false);
	}

	public static ImportMailboxItemSet moveIn(long mailboxFolderId, List<MailboxItemId> ids,
			List<MailboxItemId> expectedIds) {
		return ImportMailboxItemSet.of(mailboxFolderId, ids, expectedIds, true);
	}

	public static ImportMailboxItemSet of(long mailboxFolderId, List<MailboxItemId> ids,
			List<MailboxItemId> expectedIds, boolean deleteFromSource) {
		ImportMailboxItemSet ret = new ImportMailboxItemSet();
		ret.mailboxFolderId = mailboxFolderId;
		ret.ids = ids;
		ret.expectedIds = expectedIds;
		ret.deleteFromSource = deleteFromSource;
		return ret;
	}

}
