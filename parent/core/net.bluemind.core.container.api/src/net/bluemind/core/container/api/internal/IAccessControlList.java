/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.container.api.internal;

import java.sql.SQLException;
import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.acl.AccessControlEntry;

public interface IAccessControlList {
	public void store(final List<AccessControlEntry> entries) throws SQLException, ServerFault;

	public void add(final List<AccessControlEntry> entries) throws SQLException;

	public List<AccessControlEntry> get() throws SQLException;

	public void deleteAll() throws SQLException;

	public List<AccessControlEntry> retrieveAndStore(List<AccessControlEntry> entries) throws ServerFault;
}
