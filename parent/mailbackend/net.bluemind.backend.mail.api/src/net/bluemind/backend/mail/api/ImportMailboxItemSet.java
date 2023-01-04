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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;

@BMApi(version = "3")
@GwtIncompatible
public class ImportMailboxItemSet {

	@BMApi(version = "3")
	public static class MailboxItemId {
		public long id;

		public MailboxItemId() {
		}

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public MailboxItemId(@JsonProperty("id") long id) {
			this.id = id;
		}

		public static MailboxItemId of(long id) {
			return new MailboxItemId(id);
		}
	}

	public long mailboxFolderId;

	/**
	 * MailboxItems ids
	 */
	public List<MailboxItemId> ids;

	public boolean deleteFromSource;

	public static ImportMailboxItemSet copyIn(long mailboxFolderId, List<MailboxItemId> ids) {
		return ImportMailboxItemSet.of(mailboxFolderId, ids, false);
	}

	public static ImportMailboxItemSet moveIn(long mailboxFolderId, List<MailboxItemId> ids) {
		return ImportMailboxItemSet.of(mailboxFolderId, ids, true);
	}

	public static ImportMailboxItemSet of(long mailboxFolderId, List<MailboxItemId> ids, boolean deleteFromSource) {
		ImportMailboxItemSet ret = new ImportMailboxItemSet();
		ret.mailboxFolderId = mailboxFolderId;
		ret.ids = ids;
		ret.deleteFromSource = deleteFromSource;
		return ret;
	}

}
