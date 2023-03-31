/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.mailbox.service.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.group.hook.DefaultGroupHook;
import net.bluemind.group.hook.GroupMessage;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class MailboxGroupHook extends DefaultGroupHook {

	@Override
	public void onGroupUpdated(GroupMessage previous, GroupMessage current) throws ServerFault {
		if (!previous.group.value.name.equals(current.group.value.name)) {
			ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
			cmd.name = current.group.value.name;
			current.context.provider().instance(IContainers.class)
					.update(IMailboxAclUids.uidForMailbox(current.group.uid), cmd);
		}
	}
}
