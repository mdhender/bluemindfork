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

import net.bluemind.core.container.model.SortDescriptor;

public class MailRecordSortDefaultStrategy extends MailRecordSortStrategy {

	public MailRecordSortDefaultStrategy(SortDescriptor sortDesc) {
		super(sortDesc);
	}

	@Override
	public String queryToSort() {
		logger.info("MailRecordSortDefaultStrategy is used to sort with {}", sortDesc);
		StringBuilder query = new StringBuilder("SELECT item.id FROM t_mailbox_record rec "
				+ "INNER JOIN t_container_item item ON rec.item_id = item.id " //
				+ "INNER JOIN t_message_body body ON rec.message_body_guid = body.guid " //
				+ "WHERE item.subtree_id = ? AND item.container_id = ? ");

		if (sortDesc.filter != null && (!sortDesc.filter.must.isEmpty() || !sortDesc.filter.mustNot.isEmpty())) {
			sortDesc.filter.must.forEach(must -> query
					.append(" AND (item.flags::bit(32) & " + must.value + "::bit(32)) = " + must.value + "::bit(32) "));
			sortDesc.filter.mustNot.forEach(mustNot -> query
					.append(" AND (item.flags::bit(32) & " + mustNot.value + "::bit(32)) = 0::bit(32) "));
		}

		if (!sortDesc.fields.isEmpty()) {
			query.append(" ORDER BY ").append(getSortColumnList());
		}

		return query.toString();
	}

}
