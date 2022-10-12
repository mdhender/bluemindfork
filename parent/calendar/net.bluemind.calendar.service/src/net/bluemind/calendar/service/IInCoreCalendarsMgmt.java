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
package net.bluemind.calendar.service;

import net.bluemind.core.task.service.IServerTaskMonitor;

/**
 * In-Core calendar management api
 *
 */
public interface IInCoreCalendarsMgmt {

	/**
	 * reindex all calendars (drop current index and recreate them)
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void reindexAll(IServerTaskMonitor monitor) throws Exception;

	/**
	 * reindex a domain
	 * 
	 * @param domainUid domain
	 * @param monitor
	 * @throws Exception
	 */
	public void reindexDomain(String domainUid, IServerTaskMonitor monitor) throws Exception;

	/**
	 * reindex a calendar
	 * 
	 * @param calUid  calendar uid
	 * @param monitor
	 * @throws Exception
	 */
	public void reindex(String calUid, IServerTaskMonitor monitor) throws Exception;
}
