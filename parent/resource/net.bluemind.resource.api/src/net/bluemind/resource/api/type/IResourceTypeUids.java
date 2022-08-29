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
package net.bluemind.resource.api.type;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns specific resource-type container UIDs.
 */
@BMApi(version = "3")
@Path("/mailbox/uids")
public interface IResourceTypeUids {

	public static final String TYPE = "resources";

	/**
	 * Returns the default domain resource-type container UID
	 * 
	 * @param domainUid domain
	 * @return default domain resource-type container UID
	 */
	@GET
	@Path("{domain}/_resource_type")
	public default String identifier(@PathParam("domain") String domainUid) {
		return getIdentifier(domainUid);
	}

	public static String getIdentifier(String domainUid) {
		return domainUid;
	}

	public static String getIdentifier(ItemValue<Domain> domain) {
		return getIdentifier(domain.uid);
	}
}
