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

package net.bluemind.backend.systemconf.internal;

import java.util.Map;

import com.google.common.net.InetAddresses;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class MyNetworksValidator implements ISystemConfigurationValidator {
	private static final String PARAMETER = "mynetworks";

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);

		if (!modifications.containsKey(PARAMETER)) {
			return;
		}

		ParametersValidator.notNullAndNotEmpty(modifications.get(PARAMETER));

		for (String part : MyNetworksSanitizor.getSanitizedParts(modifications.get(PARAMETER))) {
			String[] subParts = part.split("/");

			switch (subParts.length) {
			case 2:
				int mask = -1;
				try {
					mask = Integer.parseInt(subParts[1]);
				} catch (NumberFormatException nfe) {
					throw new ServerFault("Invalid mynetworks: " + modifications.get(PARAMETER),
							ErrorCode.INVALID_PARAMETER);
				}

				if (subParts[0].contains(":")) {
					// ipv6
					if (mask < 0 || mask > 128) {
						throw new ServerFault("Invalid mynetworks: " + modifications.get(PARAMETER),
								ErrorCode.INVALID_PARAMETER);
					}
				} else {
					// ipv4
					if (mask < 0 || mask > 32) {
						throw new ServerFault("Invalid mynetworks: " + modifications.get(PARAMETER),
								ErrorCode.INVALID_PARAMETER);
					}
				}
				break;
			case 1:
				break;
			default:
				throw new ServerFault("Invalid mynetworks: " + modifications.get(PARAMETER),
						ErrorCode.INVALID_PARAMETER);
			}

			try {
				// BM-11162 - remove literal ip6 brackets
				InetAddresses.forString(subParts[0].replaceAll("[\\[\\]]", ""));
			} catch (IllegalArgumentException iae) {
				throw new ServerFault(String.format("Invalid mynetworks: %s - must contain only IP or subnet", part),
						ErrorCode.INVALID_PARAMETER);
			}
		}
	}
}
