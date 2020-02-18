package net.bluemind.announcement.service;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.announcement.provider.IAnnouncementProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */

public class UserAnnouncementsActivator implements BundleActivator {

	private static List<IAnnouncementProvider> providers = createProviders();

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	private static List<IAnnouncementProvider> createProviders() {
		RunnableExtensionLoader<IAnnouncementProvider> rel = new RunnableExtensionLoader<>();

		return rel.loadExtensions("net.bluemind.announcement.provider", "announcementprovider", "announcement_provider",
				"impl");
	}

	public static List<IAnnouncementProvider> getProviders() {
		return providers;
	}

}
