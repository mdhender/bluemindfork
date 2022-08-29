/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/deferredaction/uids")
public interface IDeferredActionContainerUids {

	public static final String TYPE = "deferredaction";
	public static final String DEFERRED_ACTION_PREFIX = "deferredaction-";

	/**
	 * Returns the deferred action container UID associated to an user
	 * 
	 * @param userUid
	 *                    the {@link net.bluemind.user.api.User} UID
	 * @return deferred action container UID
	 */
	@GET
	@Path("{uid}/_deferredaction")
	public default String getUidForUser(@PathParam("uid") String userUid) {
		return uidForUser(userUid);
	}

	public static String uidForUser(String userUid) {
		return DEFERRED_ACTION_PREFIX + userUid;
	}

	/**
	 * Returns the deferred action container UID associated to a domain
	 * 
	 * @param domainUid
	 *                      the {@link net.bluemind.domain.api.Domain} UID
	 * @return deferred action container UID
	 */
	@GET
	@Path("{uid}/_domain_deferredaction")
	public default String getUidFordomain(@PathParam("uid") String domainUid) {
		return uidForDomain(domainUid);
	}

	public static String uidForDomain(String domainUid) {
		return DEFERRED_ACTION_PREFIX + "domain-" + domainUid;
	}

	/**
	 * Returns the deferred action container type
	 *
	 * @return deferred action container type
	 */
	@GET
	@Path("_type")
	public default String getContainerType() {
		return containerType();
	}

	public static String containerType() {
		return TYPE;
	}

}
