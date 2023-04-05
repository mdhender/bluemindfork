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
package net.bluemind.calendar.hook;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class CalendarHookUser extends DefaultUserHook {

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
		if (!previous.value.login.equals(current.value.login)) {
			ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
			cmd.name = current.value.login;
			context.su().provider().instance(IContainers.class).update(ICalendarViewUids.userCalendarView(current.uid),
					cmd);
			context.su().provider().instance(IContainers.class).update(ICalendarUids.defaultUserCalendar(current.uid),
					cmd);
		}
	}
}
