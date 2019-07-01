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
package net.bluemind.addressbook.service;

import net.bluemind.core.task.service.IServerTaskMonitor;

/**
 * In-Core addressbook management api
 *
 */
public interface IInCoreAddressBooksMgmt {

	/**
	 * reindex all addressbooks (drop current index and recreate them)
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void reindexAll(IServerTaskMonitor monitor) throws Exception;

	/**
	 * reindex all addressbooks of a domain
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void reindexDomain(String domain, IServerTaskMonitor monitor) throws Exception;

	/**
	 * reindex an addressbook
	 * 
	 * @param bookUid
	 *            addressbook uid
	 * @param monitor
	 * @throws Exception
	 */
	public void reindex(String bookUid, IServerTaskMonitor monitor) throws Exception;
}
