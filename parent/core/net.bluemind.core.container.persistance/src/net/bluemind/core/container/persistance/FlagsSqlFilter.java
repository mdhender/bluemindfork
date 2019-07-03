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
package net.bluemind.core.container.persistance;

import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;

public final class FlagsSqlFilter {

	public static String filterSql(String itemTableAlias, ItemFlagFilter filter) {
		String f = "";
		if (!filter.must.isEmpty()) {
			int v = ItemFlag.value(filter.must);
			f += " AND (" + itemTableAlias + ".flags::bit(32) & " + v + "::bit(32))=" + v + "::bit(32)";
		}
		if (!filter.mustNot.isEmpty()) {
			long v = ItemFlag.value(filter.mustNot);
			f += " AND (" + itemTableAlias + ".flags::bit(32) & " + v + "::bit(32))=0::bit(32)";
		}
		return f;
	}
}
