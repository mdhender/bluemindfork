/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.model;

import java.util.Optional;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class IdQuery {

	public ItemFlagFilter filter;
	public int limit = 20;
	public int offset = 0;

	/**
	 * This is used to ensure your are not using pages of data on stuff that changed
	 * behind your back
	 */
	public long knownContainerVersion;

	public static IdQuery of(String filter, Long knownContainerVersion, Integer limit, Integer offset) {
		IdQuery idq = new IdQuery();
		idq.filter = ItemFlagFilter.fromQueryString(Optional.ofNullable(filter).orElse(""));
		idq.limit = Optional.ofNullable(limit).orElse(20);
		idq.offset = Optional.ofNullable(offset).orElse(0);
		idq.knownContainerVersion = Optional.ofNullable(knownContainerVersion).orElse(0L);
		return idq;

	}

}
