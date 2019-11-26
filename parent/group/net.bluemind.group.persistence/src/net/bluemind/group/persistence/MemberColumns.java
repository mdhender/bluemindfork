/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.group.persistence;

import net.bluemind.core.container.model.Item;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;

public final class MemberColumns {

	/**
	 * @return
	 */
	public static GroupStore.StatementValues<Item> statementValues(Item groupItem) {
		return (con, statement, index, currentRow, value) -> {
			statement.setLong(index++, groupItem.id);
			statement.setLong(index++, value.id);

			return index;
		};
	}

	public static GroupStore.EntityPopulator<Member> populator() {
		return (rs, index, value) -> {
			value.type = Type.valueOf(rs.getString(index++));
			value.uid = rs.getString(index++);

			return index;
		};
	}
}
