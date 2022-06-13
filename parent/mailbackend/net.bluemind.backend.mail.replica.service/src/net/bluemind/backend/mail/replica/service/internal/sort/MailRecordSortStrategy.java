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

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.SortDescriptor;

public abstract class MailRecordSortStrategy implements IMailRecordSortStrategy {

	protected static final Logger logger = LoggerFactory.getLogger(MailRecordSortStrategy.class);

	protected SortDescriptor sortDesc;

	MailRecordSortStrategy() {
	}

	MailRecordSortStrategy(SortDescriptor sortDesc) {
		this();
		this.sortDesc = sortDesc == null ? new SortDescriptor() : sortDesc;
	}

	protected String getSortColumnList() {
		return sortDesc.fields.stream()
				.map(f -> f.column + " " + (f.dir == SortDescriptor.Direction.Asc ? "ASC" : "DESC"))
				.collect(Collectors.joining(","));
	}

}
