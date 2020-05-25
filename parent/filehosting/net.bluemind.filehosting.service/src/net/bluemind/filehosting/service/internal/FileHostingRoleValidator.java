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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.filehosting.service.internal;

import java.util.Arrays;
import java.util.List;

import net.bluemind.authentication.service.IRoleValidator;
import net.bluemind.config.InstallationId;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;

public class FileHostingRoleValidator implements IRoleValidator {

	@Override
	public boolean valid(String domain, String role) {
		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		FileHostingInfo info = context.provider().instance(IFileHosting.class, "global.virt").info();
		if (!info.present) {
			return false;
		}

		List<Assignment> assignments = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getAssignments(domain);

		return assignments.stream().anyMatch(assignment -> assignment.tag.equals("filehosting/data"));
	}

	@Override
	public List<String> supportedRoles() {
		return Arrays.asList("canRemoteAttach", "canUseFilehosting");
	}

}
