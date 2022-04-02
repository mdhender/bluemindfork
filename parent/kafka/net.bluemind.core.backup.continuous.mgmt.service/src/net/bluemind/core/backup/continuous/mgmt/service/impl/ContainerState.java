/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class ContainerState {

	public final RangeSet<Long> itemIds;
	public final RangeSet<Long> versions;
	private String containerUid;

	public ContainerState(String contUid) {
		this.containerUid = contUid;
		itemIds = TreeRangeSet.create();
		versions = TreeRangeSet.create();
	}

	public String containerUid() {
		return containerUid;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(ContainerState.class)//
				.add("uid", containerUid)//
				.add("items", itemIds)//
				.add("versions", versions)//
				.toString();
	}

}
