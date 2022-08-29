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
package net.bluemind.directory.api;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.model.ItemValue;

/**
 * Manages Organizational Units
 * 
 * BlueMind integrates a delegated administration functionality. It allows you
 * to grant limited administration rights to administrators (who become
 * delegated administrators). Delegated administration rights can be given to
 * specific users selected according to certain criteria (job type, industry,
 * geographical area...). The resulting group is called an {@link OrgUnit}
 *
 */
@BMApi(version = "3")
@Path("/directory/_ou/{domain}")
public interface IOrgUnits extends IRestoreCrudSupport<OrgUnit> {

	/**
	 * Get {@link OrgUnit} by UID
	 * 
	 * @param uid UID
	 * @return {@link net.bluemind.core.container.model.ItemValue} containing the
	 *         {@link OrgUnit}, or null if not found
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<OrgUnit> getComplete(@PathParam(value = "uid") String uid);

	/**
	 * Get all child elements of an {@link OrgUnit}
	 * 
	 * @param uid {@link OrgUnit} UID
	 * @return {@link net.bluemind.core.container.model.ItemValue}s containing the
	 *         {@link OrgUnit}s
	 */
	@GET
	@Path("{uid}/_children")
	public List<ItemValue<OrgUnit>> getChildren(@PathParam(value = "uid") String uid);

	/**
	 * Create an {@link OrgUnit}
	 * 
	 * @param uid   Unique ID of the new {@link OrgUnit}
	 * @param value {@link OrgUnit}
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, OrgUnit value);

	/**
	 * Update an {@link OrgUnit}
	 * 
	 * @param uid   UID of the {@link OrgUnit}
	 * @param value {@link OrgUnit}
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, OrgUnit value);

	/**
	 * Delete an {@link OrgUnit}
	 * 
	 * @param uid UID of the {@link OrgUnit}
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid);

	/**
	 * Get {@link OrgUnitPath} by {@link OrgUnit} UID
	 * 
	 * @param uid UID
	 * @return {@link OrgUnitPath} or null, if not found
	 */
	@GET
	@Path("{uid}/path")
	public OrgUnitPath getPath(@PathParam(value = "uid") String uid);

	/**
	 * Get a list of {@link OrgUnitPath}s by {@link OrgUnitQuery}
	 * 
	 * @param query {@link OrgUnitQuery}
	 * @return List of matching {@link OrgUnitPath}s
	 */
	@POST
	@Path("_search")
	public List<OrgUnitPath> search(OrgUnitQuery query);

	/**
	 * Grant roles to a member of an {@link OrgUnit}
	 * 
	 * @param orgUnitUid  {@link OrgUnit} UID
	 * @param dirEntryUid UID of the member's {@link DirEntry} object
	 * @param roles       Set of roles
	 */
	@POST
	@Path("{uid}/{dirUid}/_set")
	public void setAdministratorRoles(@PathParam(value = "uid") String orgUnitUid,
			@PathParam("dirUid") String dirEntryUid, Set<String> roles);

	/**
	 * Get the roles of a member or the {@link net.bluemind.group.api.Group}s he is
	 * member of
	 * 
	 * @param orgUnitUid  {@link OrgUnit} UID
	 * @param dirEntryUid UID of the member's {@link DirEntry} object
	 * @param groups      UIDs of the {@link net.bluemind.group.api.Group}s the
	 *                    {@link DirEntry} is member of
	 * @return Set of roles
	 */
	@POST
	@Path("{uid}/{dirUid}")
	public Set<String> getAdministratorRoles(@PathParam(value = "uid") String orgUnitUid,
			@PathParam("dirUid") String dirEntryUid, List<String> groups);

	/**
	 * Get the UIDs of an {@link OrgUnit}'s administrators
	 * 
	 * @param uid {@link OrgUnit} UID
	 * @return Set of UIDs
	 */
	@GET
	@Path("{uid}/_administrators")
	public Set<String> getAdministrators(@PathParam(value = "uid") String uid);

	/**
	 * Get a list of {@link OrgUnitPath}s by the UID of an administrator or the
	 * {@link net.bluemind.group.api.Group}s he is member of
	 * 
	 * @param administrator Administrator UID
	 * @param groups        UIDs of the {@link net.bluemind.group.api.Group}s the
	 *                      {@link DirEntry} is member of
	 * @return List of {@link OrgUnitPath}s
	 */
	@POST
	@Path("_byAdmin")
	public List<OrgUnitPath> listByAdministrator(@QueryParam("administrator") String administrator,
			List<String> groups);

	/**
	 * Remove an administrator from an {@link OrgUnit}
	 * 
	 * @param administrator Administrator UID
	 */
	@DELETE
	@Path("_deleteadmin")
	public void removeAdministrator(@QueryParam("administrator") String administrator);

}
