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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.core.container.model.ItemIdentifier;

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

		@Override
		public String toString() {
			return "[" + source + " -> " + destination + "]";
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

	@Override
	public String toString() {
		return "ImportStatus{r: " + status + ", work: " + Arrays.toString(doneIds.toArray()) + "}";
	}

	@GwtIncompatible
	public static ImportMailboxItemsStatus fromTransferResult(List<Long> sourceIds, List<ItemIdentifier> destIds) {
		ImportMailboxItemsStatus ret = new ImportMailboxItemsStatus();

		if (sourceIds == null || destIds == null || destIds.isEmpty()) {
			ret.status = ImportStatus.ERROR;
			ret.doneIds = Collections.emptyList();
		} else {
			if (sourceIds.size() > destIds.size()) {
				ret.status = ImportStatus.PARTIAL;
			} else {
				ret.status = ImportStatus.SUCCESS;
			}
			ret.doneIds = new ArrayList<>(destIds.size());
			Iterator<Long> srcIt = sourceIds.iterator();
			Iterator<ItemIdentifier> dstIt = destIds.iterator();
			while (dstIt.hasNext()) {
				ret.doneIds.add(ImportedMailboxItem.of(srcIt.next(), dstIt.next().id));
			}
		}
		return ret;
	}

}
