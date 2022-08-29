/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.directory.api;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/directory/{domain}/{entryUid}/mgmt")
public interface IDirEntryMaintenance {

	/**
	 * Retrieve available {@link MaintenanceOperation} applicable to a
	 * {@link DirEntry}
	 * 
	 * @return Set of {@link MaintenanceOperation}
	 */
	@GET
	@Path("_maintenance")
	List<MaintenanceOperation> getAvailableOperations();

	/**
	 * Execute {@link MaintenanceOperation} in "Repair" mode
	 * 
	 * @param opIdentifiers Set of {@link MaintenanceOperation} identifiers
	 * @return Reference to the net.bluemind.core.task.api.TaskRef
	 */
	@POST
	@Path("_maintenance/repair")
	TaskRef repair(RepairConfig config);

}
