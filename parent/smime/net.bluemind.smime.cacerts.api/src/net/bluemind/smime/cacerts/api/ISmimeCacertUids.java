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
package net.bluemind.smime.cacerts.api;

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
 * Returns specific notes container UIDs
 */
@BMApi(version = "3")
@Path("/smime_cacerts/uids")
public interface ISmimeCacertUids {

	public static final String TYPE = "smime_cacerts";

	/**
	 * Returns the UID of domain-created certificates list
	 * 
	 * @param uniqueUid unique certificates list UID
	 * @return UID
	 */
	@GET
	@Path("{uid}/_domain")
	public default String getDomainCreatedCerts(@PathParam("uid") String uniqueUid) {
		return domainCreatedCerts(uniqueUid);
	}

	public static String domainCreatedCerts(String uid) {
		return TYPE + ":domain_" + uid;
	}
}
