/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.backup.continuous.mgmt.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/continuous/mgmt")
public interface IContinuousBackupMgmt {

	/**
	 * 
	 * This is the main 'clone-init' task which pushes all data to a kafka topic.
	 * 
	 * @param options
	 * @return
	 */
	@Path("_sync")
	@POST
	TaskRef syncWithStore(BackupSyncOptions options);

	/**
	 * Reads the stream and perform repairs if required
	 */
	@Path("_checkrepair")
	@POST
	TaskRef checkAndRepair(CheckAndRepairOptions options);

	/**
	 * Registers this instance installation id inside zookeeper (as a file
	 * bluemind.net/forest/${forestId}/${installationId}). CRP will only considers
	 * the kafka topics of registered installations.
	 * 
	 * When an instance is forked, it gets a new installationId which will be
	 * ignored by CRP.
	 * 
	 * @param forestId
	 * @return
	 */
	@Path("{forestId}/_join")
	@POST
	TaskRef join(@PathParam("forestId") String forestId);

}
