/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.service.internal.sort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.SortDescriptor;

public abstract class MailRecordSortStrategyFactory {

	protected static final Logger logger = LoggerFactory.getLogger(MailRecordSortStrategyFactory.class);

	public static IMailRecordSortStrategy get(SortDescriptor sortDesc) {

		MailboxRecordsSort mailSortEngine = getRecordsSortStrategy(sortDesc);

		switch (mailSortEngine) {
		case OPTIMIZED:
			return new MailRecordSortOptimStrategy(sortDesc);
		case DEFAULT:
			return new MailRecordSortDefaultStrategy(sortDesc);
		default:
			throw new ServerFault(String.format("Mail Sort Strategy '%s' is not valid.", mailSortEngine));
		}

	}

	private static MailboxRecordsSort getRecordsSortStrategy(SortDescriptor sortDesc) {
		if (sortDesc != null && (MailRecordSortOptimStrategy.isOptimizedSort(sortDesc)
				|| MailRecordSortOptimStrategy.isOptimizedFilter(sortDesc))) {
			return MailboxRecordsSort.OPTIMIZED;
		}

		return MailboxRecordsSort.DEFAULT;
	}

	private enum MailboxRecordsSort {
		OPTIMIZED("s_mailbox_records"), DEFAULT("t_mailbox_records");

		private final String description;

		MailboxRecordsSort(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

}
