/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class ServerSortTests {
	@Test
	public void serverSortTests() {
		ItemValue<Server> core = ItemValue.create(Item.create("core", 0),
				Server.tagged("0.0.0.1", TagDescriptor.bm_core.getTag()));
		ItemValue<Server> esfull = ItemValue.create(Item.create("esfull", 1),
				Server.tagged("0.0.0.2", TagDescriptor.bm_es_data.getTag(), TagDescriptor.bm_es.getTag()));
		ItemValue<Server> esdata = ItemValue.create(Item.create("esdata", 2),
				Server.tagged("0.0.0.3", TagDescriptor.bm_es_data.getTag()));
		ItemValue<Server> other = ItemValue.create(Item.create("esdata", 3),
				Server.tagged("0.0.0.4", TagDescriptor.bm_filehosting.getTag()));

		List<ItemValue<Server>> servers = new ArrayList<>(List.of(other, esdata, esfull, core));
		servers.sort(Comparator.comparing(iv -> iv.value));
		assertEquals(0, servers.get(0).internalId);
		assertEquals(1, servers.get(1).internalId);
	}
}
