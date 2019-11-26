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
package net.bluemind.device.service.internal;

import java.sql.SQLException;
import java.util.List;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevices;
import net.bluemind.device.persistence.DeviceStore;

public class DevicesService implements IDevices {
	private DeviceStore store;
	private BmContext context;

	public DevicesService(BmContext context) {
		this.context = context;
		store = new DeviceStore(context.getDataSource(), null);
	}

	@Override
	public List<Device> listWiped() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("DevicesService is only available to admin0", ErrorCode.PERMISSION_DENIED);
		}

		try {
			return store.getWipedDevice();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
