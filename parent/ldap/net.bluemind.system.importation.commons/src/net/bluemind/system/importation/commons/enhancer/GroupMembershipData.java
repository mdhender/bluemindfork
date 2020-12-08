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
package net.bluemind.system.importation.commons.enhancer;

import com.google.common.collect.Sets.SetView;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;

public class GroupMembershipData {
	public final ItemValue<Group> group;
	public final SetView<Member> membersAdded;
	public final SetView<Member> membersRemoved;

	public GroupMembershipData(ItemValue<Group> group, SetView<Member> membersAdded, SetView<Member> membersRemoved) {
		this.group = group;
		this.membersAdded = membersAdded;
		this.membersRemoved = membersRemoved;
	}
}
