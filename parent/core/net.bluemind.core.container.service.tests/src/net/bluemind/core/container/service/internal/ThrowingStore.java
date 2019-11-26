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

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;

public class ThrowingStore extends AbstractItemValueStore<Dummy> {

	public ThrowingStore(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public void create(Item item, Dummy value) throws SQLException {
		throw new SQLException("ok");
	}

	@Override
	public void update(Item item, Dummy value) throws SQLException {
		throw new SQLException("ok");
	}

	@Override
	public Dummy get(Item item) throws SQLException {
		throw new SQLException("ok");
	}

	@Override
	public void delete(Item item) throws SQLException {
		throw new SQLException("ok");
	}

	@Override
	public void deleteAll() throws SQLException {
		throw new SQLException("ok");
	}

}
