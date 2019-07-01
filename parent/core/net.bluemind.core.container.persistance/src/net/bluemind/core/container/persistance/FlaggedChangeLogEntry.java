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
package net.bluemind.core.container.persistance;

import java.util.Collection;
import java.util.EnumSet;

import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;

public class FlaggedChangeLogEntry extends ChangeLogEntry {

	public Collection<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);

	public boolean match(ItemFlagFilter filter) {
		for (ItemFlag f : filter.must) {
			if (!flags.contains(f)) {
				return false;
			}
		}
		for (ItemFlag f : filter.mustNot) {
			if (flags.contains(f)) {
				return false;
			}
		}
		return true;
	}
}
