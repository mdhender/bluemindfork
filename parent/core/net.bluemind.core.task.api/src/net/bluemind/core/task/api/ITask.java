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
package net.bluemind.core.task.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;

/**
 * @param taskId task identifier
 */
@BMApi(version = "3")
@Path("/tasks/{taskId}")
public interface ITask {

	/**
	 * retrieve task status
	 * 
	 * @return {@code TaskStatus}
	 */
	@GET
	TaskStatus status();

	/**
	 * retrieve task log (open until task end)
	 * 
	 * @return Stream of log
	 */
	@GET
	@Path("_log")
	Stream log();

	@GET
	@Path("_currentLogs")
	public List<String> getCurrentLogs();

}
