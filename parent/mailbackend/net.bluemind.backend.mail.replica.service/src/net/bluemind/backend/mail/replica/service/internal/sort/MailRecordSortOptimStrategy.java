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

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.SortDescriptor;

public class MailRecordSortOptimStrategy extends MailRecordSortStrategy {

	static final List<String> OPTIMIZED_COLUMNS = Arrays.asList("internal_date", "subject", "size", "sender");

	public MailRecordSortOptimStrategy(SortDescriptor sortDesc) {
		super(sortDesc);
	}

	@Override
	public String queryToSort() {
		StringBuilder query = new StringBuilder(
				"SELECT rec.item_id FROM s_mailbox_record rec WHERE rec.container_id = ? ");

		if (isFilteredOnNotDeletedAndImportant(sortDesc)) {
			query.append(" AND rec.flagged is TRUE ");
		} else if (isFilteredOnNotDeletedAndNotSeen(sortDesc)) {
			query.append(" AND rec.unseen is TRUE ");
		}

		if (!sortDesc.fields.isEmpty()) {
			sortDesc.fields.stream().filter(f -> "internal_date".equals(f.column)).forEach(f -> f.column = "date");
			query.append(" ORDER BY ").append(getSortColumnList());
		}

		return query.toString();
	}

	public static boolean isOptimizedSort(SortDescriptor sortDesc) {
		return sortDesc.fields.size() == 1 && OPTIMIZED_COLUMNS.contains(sortDesc.fields.get(0).column);
	}

	public static boolean isOptimizedFilter(SortDescriptor sortDesc) {
		return isFilteredOnNotDeletedAndNotSeen(sortDesc) || isFilteredOnNotDeletedAndImportant(sortDesc)
				|| isFilteredOnNotDeleted(sortDesc);
	}

	public static boolean isFilteredOnNotDeletedAndNotSeen(SortDescriptor sortDesc) {
		return sortDesc.filter != null && sortDesc.filter.mustNot.size() == 2
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Seen)
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Deleted);
	}

	public static boolean isFilteredOnNotDeletedAndImportant(SortDescriptor sortDesc) {
		return sortDesc.filter != null && sortDesc.filter.must.size() == 1 && sortDesc.filter.mustNot.size() == 1
				&& sortDesc.filter.must.stream().anyMatch(f -> f == ItemFlag.Important)
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Deleted);
	}

	public static boolean isFilteredOnNotDeleted(SortDescriptor sortDesc) {
		return sortDesc.filter != null && sortDesc.filter.mustNot.size() == 1
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Deleted);
	}

}
