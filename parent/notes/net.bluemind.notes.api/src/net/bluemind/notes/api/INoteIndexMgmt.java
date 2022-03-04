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
package net.bluemind.notes.api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;

/**
 * Notes indexing API
 *
 */
@BMApi(version = "3")
@Path("/mgmt/notes/{containerUid}")
public interface INoteIndexMgmt {

	/**
	 * Reindex all Note containers
	 * 
	 * @return {@link net.bluemind.core.task.api.TaskRef} which can be used to track
	 *         this asynchronous operation
	 * @throws ServerFault
	 */
	@POST
	@Path("_reindex")
	public TaskRef reindexAll() throws ServerFault;

	/**
	 * Reindex a Note container
	 * 
	 * @param noteContainerUid Unique Note Container ID
	 * @return {@link net.bluemind.core.task.api.TaskRef} which can be used to track
	 *         this asynchronous operation
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/_reindex")
	public TaskRef reindex(@PathParam("uid") String noteContainerUid) throws ServerFault;

}