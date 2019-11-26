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
package net.bluemind.todolist.service;

import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService.IWeightSeedProvider;
import net.bluemind.todolist.api.VTodo;

public class VTodoWeight {

	private static final IWeightSeedProvider<VTodo> prov = todo -> {
		if (todo.due != null) {
			return BmDateTimeWrapper.toTimestamp(todo.due.iso8601, "UTC");
		}
		return 0L;
	};

	private static final IWeightProvider wProv = seed -> {
		return Long.MAX_VALUE - Math.abs(System.currentTimeMillis() - seed);
	};

	public static IWeightSeedProvider<VTodo> seedProvider() {
		return prov;
	}

	public static IWeightProvider weigthProvider() {
		return wProv;
	}

}
