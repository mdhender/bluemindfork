/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.repair;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;

public interface ContainerRepairOp {

	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor);

	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor);

	public DirEntry.Kind supportedKind();

	public default void verifyContainer(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor,
			Runnable maintenance, String containerUid) {

		ContainerDescriptor container = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class).getIfPresent(containerUid);

		if (container == null) {
			maintenance.run();
		}
	}

}
