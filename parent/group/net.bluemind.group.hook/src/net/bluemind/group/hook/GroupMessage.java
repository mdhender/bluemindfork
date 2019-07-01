/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.group.hook;

import java.util.List;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;

public final class GroupMessage {

	public SecurityContext securityContext;
	public ContainerDescriptor container;
	public ItemValue<Group> group;
	public List<Member> members;

	public GroupMessage(ItemValue<Group> group, SecurityContext sc, Container c) {
		this.group = group;
		this.securityContext = sc;
		this.container = descriptor(c);
		this.members = null;
	}

	private static ContainerDescriptor descriptor(Container c) {
		return ContainerDescriptor.create(c.uid, c.name, c.owner, c.type, c.domainUid, c.defaultContainer);
	}

	public GroupMessage(ItemValue<Group> group, SecurityContext sc, Container c, List<Member> members) {
		this.group = group;
		this.securityContext = sc;
		this.container = descriptor(c);
		this.members = members;
	}
}
