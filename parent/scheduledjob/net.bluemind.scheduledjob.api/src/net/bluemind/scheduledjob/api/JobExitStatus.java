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
package net.bluemind.scheduledjob.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum JobExitStatus {

	/**
	 * Job is running
	 */
	IN_PROGRESS,

	/**
	 * Successful execution
	 */
	SUCCESS,

	/**
	 * The task reported some problems but the work was accomplished.
	 */
	COMPLETED_WITH_WARNINGS,

	/**
	 * The task failed. Most of its work was not done.
	 */
	FAILURE,

	/**
	 * The task never run or all records was purge.
	 */
	UNKNOWN,
	/**
	 * The task has been interrupted.
	 */
	INTERRUPTED;

}
