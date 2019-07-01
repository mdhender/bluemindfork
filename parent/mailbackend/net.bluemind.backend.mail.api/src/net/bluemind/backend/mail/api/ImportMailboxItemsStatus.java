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

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ImportMailboxItemsStatus {

	@BMApi(version = "3")
	public static class ImportedMailboxItem {

		public long source;
		public long destination;

		public static ImportedMailboxItem of(long source, long destination) {
			ImportedMailboxItem ret = new ImportedMailboxItem();
			ret.source = source;
			ret.destination = destination;
			return ret;
		}

	}

	@BMApi(version = "3")
	public static enum ImportStatus {
		SUCCESS, PARTIAL, ERROR
	}

	/**
	 * Source MailboxItem id as key, destination MailboxItem id as value
	 */
	public List<ImportedMailboxItem> doneIds = Collections.emptyList();
	public ImportStatus status;

}
