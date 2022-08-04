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
package net.bluemind.directory.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirEntry;

public interface IInCoreDirectory {

	/**
	 * Creates a new {@link DirEntry} entry.
	 * 
	 * @param path  path of the entry
	 * @param entry value of the entry
	 * 
	 * @throws ServerFault
	 */

	public void create(String uid, DirEntry entry) throws ServerFault;

	/**
	 * Modifies an existing {@link DirEntry} entry.
	 * 
	 * @param path  path of the entry
	 * @param entry value of the entry
	 * @throws ServerFault
	 */
	public void update(String uid, DirEntry entry) throws ServerFault;

	/**
	 * Delete entry
	 * 
	 * @param path
	 * @throws ServerFault
	 */
	public void delete(String uid, String kind) throws ServerFault;

	public void updateAccountType(String uid, AccountType accountType) throws ServerFault;

	ContainerChangeset<ItemIdentifier> fullChangeset() throws ServerFault;
}
