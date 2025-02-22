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
package net.bluemind.role.api;

import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * Role API.
 */
@BMApi(version = "3")
@Path("/roles")
public interface IRoles {

	/**
	 * Get all available {@link RoleCategory}s. Roles are grouped by
	 * {@link RoleCategory}s.
	 * 
	 * @return Set of {@link RoleCategory}s
	 * @throws ServerFault
	 *                         Common error object
	 */
	@GET
	@Path("categories")
	public Set<RolesCategory> getRolesCategories() throws ServerFault;

	/**
	 * Get all available {@link RoleDescriptor}.
	 * 
	 * @return Set of {@link RoleDescriptor}s
	 * @throws ServerFault
	 *                         Common error object
	 */
	@GET
	@Path("descriptors")
	public Set<RoleDescriptor> getRoles() throws ServerFault;

}
