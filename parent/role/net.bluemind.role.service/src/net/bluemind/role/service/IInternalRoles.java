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
package net.bluemind.role.service;

import java.util.List;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.role.api.IRoles;

public interface IInternalRoles extends IRoles {

	public Set<String> filter(Set<String> roles) throws ServerFault;

	public Set<String> resolve(Set<String> roles);

	public Set<String> resolveSelf(List<String> roles);

	public Set<String> resolveDirEntry(List<String> roles, DirEntry entry);

}
