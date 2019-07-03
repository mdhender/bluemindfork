/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2014
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.monitoring.api.IMonitoring;
import net.bluemind.monitoring.api.MonitoringRoles;
import net.bluemind.monitoring.service.internal.MonitoringService;

public class MonitoringServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IMonitoring> {

	@Override
	public Class<IMonitoring> factoryClass() {
		return IMonitoring.class;
	}

	@Override
	public IMonitoring instance(BmContext context, String... params) throws ServerFault {
		new RBACManager(context).check(MonitoringRoles.ROLE_MONITORING);
		return new MonitoringService(context);
	}

}