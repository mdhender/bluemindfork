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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns specific owner subscription container UIDs.
 */
@BMApi(version = "3")
@Path("/owner_subscriptions/uids")
public interface IOwnerSubscriptionUids {

	public static final String TYPE = "owner_subscriptions";
	public static final String REPAIR_OP_ID = "owner.subscriptions";

	/**
	 * Returns the owner subscription container UID
	 * 
	 * @param ownerUid  owner
	 * @param domainUid domain
	 * @return owner subscription container uid
	 */
	@GET
	@Path("{uid}/{domain}/_subscription")
	public default String identifier(@PathParam("uid") String ownerUid, @PathParam("domain") String domainUid) {
		return getIdentifier(ownerUid, domainUid);
	}

	public static String getIdentifier(String ownerUid, String domainUid) {
		return "owner_subscriptions_" + ownerUid + "_at_" + domainUid;
	}

	public static String subscriptionUid(String containerUid, String ownerUid) {
		return "sub-of-" + ownerUid + "-to-" + containerUid;
	}

}
