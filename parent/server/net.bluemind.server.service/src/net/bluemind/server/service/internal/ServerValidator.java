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
package net.bluemind.server.service.internal;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.Server;

public class ServerValidator {

	public void validate(Server server) throws ServerFault {

		if (server == null) {
			throw new ServerFault("Server is null", ErrorCode.INVALID_PARAMETER);
		}

		if (StringUtils.isBlank(server.name)) {
			throw new ServerFault("Server.name must be set ", ErrorCode.INVALID_PARAMETER);
		}
		if (StringUtils.isBlank(server.ip) && StringUtils.isBlank(server.fqdn)) {
			throw new ServerFault("Server.ip or Server.fqdn must be set", ErrorCode.INVALID_PARAMETER);
		}
	}

}
