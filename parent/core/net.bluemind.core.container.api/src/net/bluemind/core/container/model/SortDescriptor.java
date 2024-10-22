/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SortDescriptor {

	@BMApi(version = "3")
	public enum Direction {
		Asc, Desc;
	}

	@BMApi(version = "3")
	public static class Field {
		public String column;
		public Direction dir;

		public static Field create(String column, Direction direction) {
			Field field = new Field();
			field.column = column;
			field.dir = direction;
			return field;
		}
	}

	public List<Field> fields = Collections.emptyList();

	public ItemFlagFilter filter = null;

	@Override
	public String toString() {
		String sortedString = fields.isEmpty() ? null
				: "Sort on " + fields.stream().map(f -> "" + f.column + " " + f.dir).collect(Collectors.joining(", "));

		String filterString = filter == null ? null : "Filter on " + ItemFlagFilter.toQueryString(filter);

		if (sortedString != null && filterString != null) {
			return sortedString + " and " + filterString;
		}

		if (sortedString != null) {
			return sortedString;
		}

		return filterString;
	}
}
