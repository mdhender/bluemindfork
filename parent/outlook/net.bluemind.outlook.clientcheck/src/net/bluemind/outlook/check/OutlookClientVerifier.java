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
package net.bluemind.outlook.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.filter.RestFilterAdapter;
import net.bluemind.outlook.check.IClientCompatibilityCheck.ClientCompatibility;

public class OutlookClientVerifier extends RestFilterAdapter {

	private static final Logger logger = LoggerFactory.getLogger(OutlookClientVerifier.class);

	@Override
	public AsyncHandler<RestResponse> authorized(RestRequest request, SecurityContext securityContext,
			AsyncHandler<RestResponse> responseHandler) {
		ClientCompatibility clientCheck = isCompatible(request, securityContext);

		if (!clientCheck.compatible) {
			logger.warn("Rejecting client: {}", clientCheck.message);
			responseHandler.success(RestResponse.incompatibleClient(clientCheck.message));
			return null;
		} else {
			return responseHandler;
		}
	}

	private ClientCompatibility isCompatible(RestRequest request, SecurityContext securityContext) {

		String origin = securityContext.getOrigin();

		if (isOutlookRequest(origin)) {
			// Outlook connector is deprecated in BM 5
			String coreVersion = getCoreVersion();
			return new ClientCompatibility(false,
					String.format("Client version %s is not compatible with server version %s", origin, coreVersion));
		}
		return ClientCompatibility.Ok();
	}

	private String getCoreVersion() {
		return BMVersion.getVersion();
	}

	private boolean isOutlookRequest(String origin) {
		return origin != null && origin.startsWith("bm-connector-outlook");
	}

}