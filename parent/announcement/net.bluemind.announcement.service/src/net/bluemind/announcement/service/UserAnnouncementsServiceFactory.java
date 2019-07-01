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
package net.bluemind.announcement.service;

import java.util.List;

import net.bluemind.announcement.api.IUserAnnouncements;
import net.bluemind.announcement.provider.IAnnouncementProvider;
import net.bluemind.announcement.service.internal.UserAnnouncementService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class UserAnnouncementsServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IUserAnnouncements> {

	@Override
	public Class<IUserAnnouncements> factoryClass() {
		return IUserAnnouncements.class;
	}

	@Override
	public IUserAnnouncements instance(BmContext context, String... params) throws ServerFault {

		RunnableExtensionLoader<IAnnouncementProvider> rel = new RunnableExtensionLoader<IAnnouncementProvider>();

		List<IAnnouncementProvider> providers = rel.loadExtensions("net.bluemind.announcement.provider",
				"announcementprovider", "announcement_provider", "impl");

		return new UserAnnouncementService(context, providers);
	}

}
