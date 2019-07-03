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

package net.bluemind.backend.postfix;

import net.bluemind.backend.postfix.internal.maps.events.EventProducer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.group.hook.GroupMessage;
import net.bluemind.group.hook.IGroupHook;

public class GroupHook implements IGroupHook {
	@Override
	public void onGroupCreated(GroupMessage created) {
	}

	@Override
	public void onGroupUpdated(GroupMessage previous, GroupMessage current) throws ServerFault {
	}

	@Override
	public void onGroupDeleted(GroupMessage deleted) throws ServerFault {
	}

	@Override
	public void onAddMembers(GroupMessage group) throws ServerFault {
		EventProducer.dirtyMaps();
	}

	@Override
	public void onRemoveMembers(GroupMessage group) throws ServerFault {
		EventProducer.dirtyMaps();
	}
}
