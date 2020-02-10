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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.filter.RestFilterAdapter;
import net.bluemind.outlook.check.IClientCompatibilityCheck.ClientCompatibility;

public class OutlookClientVerifier extends RestFilterAdapter {

	private final Pattern clientVersion = Pattern.compile("^bm-connector-outlook-(.+?)\\s.*");
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
			String coreVersion = getCoreVersion();
			if (!isValid(origin, coreVersion)) {
				return new ClientCompatibility(false, String
						.format("Client version %s is not compatible with server version %s", origin, coreVersion));
			}
		}
		return ClientCompatibility.Ok();
	}

	private String getCoreVersion() {
		return BMVersion.getVersion();
	}

	private boolean isValid(String origin, String coreVersion) {
		if (origin.contains("qualifier") || coreVersion.contains("qualifier")) {
			return true;
		}
		if (origin.startsWith("bm-connector-outlook-DEV")) {
			return true;
		}
		if (origin.equals(coreVersion)) {
			return true;
		}
		Matcher clientVersionMatcher = clientVersion.matcher(origin);
		if (clientVersionMatcher.find()) {
			VersionInfo client = VersionInfo.create(clientVersionMatcher.group(1));
			return client.greaterThan(VersionInfo.create("4.1.47241"));
		}
		return false;
	}

	private boolean isOutlookRequest(String origin) {
		return origin != null && origin.startsWith("bm-connector-outlook");
	}

}