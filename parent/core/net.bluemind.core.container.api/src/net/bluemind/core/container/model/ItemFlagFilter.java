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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

	/**
	 * Creates from a string like "-deleted,-seen,+important" (not case sensitive)
	 * 
	 * @param filters
	 * @return
	 */
	public static ItemFlagFilter fromQueryString(String filters) {
		// we don't use guava here to avoid creating a mess in gwt build
		Map<String, ItemFlag> quickMatch = Arrays.stream(ItemFlag.values())
				.collect(Collectors.toMap(k -> k.name().toLowerCase(), k -> k));

		String[] chunks = filters.split(",");
		ItemFlagFilter flagFilter = ItemFlagFilter.create();
		for (String chunk : chunks) {
			if (chunk.length() > 2) {
				String flagPart = chunk.substring(1).toLowerCase();
				switch (chunk.charAt(0)) {
				case '+':
					Optional.ofNullable(quickMatch.get(flagPart)).ifPresent(flagFilter::must);
					break;
				case '-':
					Optional.ofNullable(quickMatch.get(flagPart)).ifPresent(flagFilter::mustNot);
					break;
				default:
					break;
				}
			}
		}
		return flagFilter;
	}

	public static String toQueryString(ItemFlagFilter filter) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ItemFlag iflag : filter.must) {
			if (!first) {
				sb.append(',');
			}
			sb.append('+').append(iflag.name().toLowerCase());
			first = false;
		}
		for (ItemFlag iflag : filter.mustNot) {
			if (!first) {
				sb.append(',');
			}
			sb.append('-').append(iflag.name().toLowerCase());
			first = false;
		}
		return sb.toString();
	}

}
