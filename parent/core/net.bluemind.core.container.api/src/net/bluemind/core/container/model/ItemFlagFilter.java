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
public class ItemFlagFilter {

	public Collection<ItemFlag> must = EnumSet.noneOf(ItemFlag.class);
	public Collection<ItemFlag> mustNot = EnumSet.noneOf(ItemFlag.class);

	public static ItemFlagFilter create() {
		return new ItemFlagFilter();
	}

	public static ItemFlagFilter all() {
		return create();
	}

	public ItemFlagFilter must(ItemFlag... flags) {
		for (ItemFlag f : flags) {
			must.add(f);
		}
		return this;
	}

	public boolean matchAll() {
		return must.isEmpty() && mustNot.isEmpty();
	}

	public ItemFlagFilter mustNot(ItemFlag... flags) {
		for (ItemFlag f : flags) {
			mustNot.add(f);
		}
		return this;
	}

}
