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
package net.bluemind.announcement.service.internal;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.announcement.api.Announcement;
import net.bluemind.announcement.api.IUserAnnouncements;
import net.bluemind.announcement.provider.IAnnouncementProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;

public class UserAnnouncementService implements IUserAnnouncements {

	private List<IAnnouncementProvider> providers;
	private SecurityContext ctx;

	public UserAnnouncementService(BmContext context, List<IAnnouncementProvider> providers) {
		this.providers = providers;
		ctx = context.getSecurityContext();
	}

	@Override
	public List<Announcement> get() throws ServerFault {
		// TODO secure

		List<Announcement> ret = new ArrayList<Announcement>();
		for (IAnnouncementProvider provider : providers) {
			ret.addAll(provider.getAnnouncements(ctx));
		}

		return ret;
	}

}
