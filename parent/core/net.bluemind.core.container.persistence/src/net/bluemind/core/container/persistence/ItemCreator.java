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
package net.bluemind.core.container.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;

public class ItemCreator {

	public static final Creator<Item> INSTANCE = new Creator<Item>() {

		@Override
		public Item create(ResultSet con) throws SQLException {
			return new Item();
		}

	};

}
