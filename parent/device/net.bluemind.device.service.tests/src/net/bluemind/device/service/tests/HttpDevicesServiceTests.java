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
package net.bluemind.device.service.tests;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.device.api.IDevice;
import net.bluemind.device.api.IDevices;

public class HttpDevicesServiceTests extends DeviceServiceTests {

	@Override
	public IDevices getDevicesService(SecurityContext sc) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sc.getSessionId())
				.instance(IDevices.class);
	}

	@Override
	public IDevice getDeviceService(SecurityContext context, String userUid) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", context.getSessionId())
				.instance(IDevice.class, userUid);
	}

}
