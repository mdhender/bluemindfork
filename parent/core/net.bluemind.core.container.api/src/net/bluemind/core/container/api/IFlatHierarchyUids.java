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
package net.bluemind.core.container.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns the flat container hierarchy container UID.
 */
@BMApi(version = "3")
@Path("/flat_hierarchy/uids")
public interface IFlatHierarchyUids {

	public static final String TYPE = "container_flat_hierarchy";
	public static final String REPAIR_OP_ID = "flat.hierarchy";
	public static final String REPAIR_PF_OP_ID = "flat.hierarchy.pf";

	/**
	 * Returns the flat container hierarchy container UID
	 * 
	 * @param ownerUid
	 *                      the owner UID
	 * @param domainUid
	 *                      the domain
	 * @return flat container hierarchy container UID
	 */
	@GET
	@Path("{uid}/{domain}/_entry_hierarchy")
	public default String identifier(@PathParam("uid") String ownerUid, @PathParam("domain") String domainUid) {
		return getIdentifier(ownerUid, domainUid);
	}

	public static String getIdentifier(String ownerUid, String domainUid) {
		return "container_flat_hierarchy_" + ownerUid + "_at_" + domainUid;
	}

}
