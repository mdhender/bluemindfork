/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.metrics.alerts.api;

import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

/**
 * This API provides "status oriented" (ok, warn, failed) monitoring results.
 * Single checks can be re-executed thanks to
 * {@link IProductChecks#check(String)} and the last result can be retrieved
 * with {@link IProductChecks#lastResult(String)}.
 *
 */
@BMApi(version = "3", internal = true)
@Path("/check")
public interface IProductChecks {

	/**
	 * Returns the list of executed check names.
	 * 
	 * @return check names with available results
	 */
	@GET
	Set<String> availableChecks();

	/**
	 * Returns the result of the last execution of a given check
	 * 
	 * @param checkName
	 * @return a result or null if the check was never executed
	 */
	@GET
	@Path("{checkName}")
	CheckResult lastResult(@PathParam("checkName") String checkName);

	/**
	 * Starts asynchronously a product check with the given name.
	 * 
	 * @param checkName the name of the product check to start
	 * @return a TaskRef suitable for tracking the progress of the check.
	 */
	@POST
	@Path("{checkName}")
	TaskRef check(@PathParam("checkName") String checkName);

}
