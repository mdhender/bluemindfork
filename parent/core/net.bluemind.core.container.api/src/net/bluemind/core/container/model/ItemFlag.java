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
package net.bluemind.core.container.model;

import java.util.Collection;
import java.util.EnumSet;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum ItemFlag {

	Seen(1 << 0),

	Deleted(1 << 1),

	Important(1 << 2);

	public static final Collection<ItemFlag> SEEN = EnumSet.of(Seen);

	public final int value;

	private ItemFlag(int v) {
		this.value = v;
	}

	public static int value(Collection<ItemFlag> flags) {
		int r = 0;
		for (ItemFlag f : flags) {
			r |= f.value;
		}
		return r;
	}

	public static Collection<ItemFlag> flags(int v) {
		EnumSet<ItemFlag> ret = EnumSet.noneOf(ItemFlag.class);
		for (ItemFlag f : ItemFlag.values()) {
			if ((v & f.value) == f.value) {
				ret.add(f);
			}
		}
		return ret;
	}
}
