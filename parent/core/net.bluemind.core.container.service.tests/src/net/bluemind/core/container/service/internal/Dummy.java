/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.core.container.service.internal;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class Dummy {

	public static final Columns DUMMY_COLUMNS = Columns.create().col("seen").col("deleted");

	public static final JdbcAbstractStore.Creator<Dummy> CREATOR = (rs) -> new Dummy();

	public static final JdbcAbstractStore.EntityPopulator<Dummy> POPUL = (rs, index, value) -> {
		value.seen = rs.getBoolean(index++);
		value.deleted = rs.getBoolean(index++);
		return index;
	};

	public static final JdbcAbstractStore.StatementValues<Dummy> value(Item it) {
		return (con, st, index, cur, value) -> {
			st.setBoolean(index++, value.seen);
			st.setBoolean(index++, value.deleted);
			st.setLong(index++, it.id);
			return index;
		};
	}

	public boolean seen;
	public boolean deleted;

}
