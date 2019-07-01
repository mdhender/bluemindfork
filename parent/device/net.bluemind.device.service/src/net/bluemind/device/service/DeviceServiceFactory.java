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
package net.bluemind.device.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.device.api.IDevice;
import net.bluemind.device.service.internal.DeviceService;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.hornetq.client.Topic;

public class DeviceServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IDevice> {

	public DeviceServiceFactory() {
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.HOOKS_DEVICE);
			}
		});
	}

	@Override
	public Class<IDevice> factoryClass() {
		return IDevice.class;
	}

	@Override
	public IDevice instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String userUid = params[0];
		String containerId = "device:" + userUid;
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("Devices container (" + containerId + ") not found for user " + userUid,
					ErrorCode.NOT_FOUND);
		}

		DeviceService service = new DeviceService(context, container, userUid);

		return service;
	}
}
