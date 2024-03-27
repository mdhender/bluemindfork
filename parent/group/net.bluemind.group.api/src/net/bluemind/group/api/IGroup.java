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
package net.bluemind.group.api;

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
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirEntryExtIdSupport;

@BMApi(version = "3")
@Path("/groups/{domainUid}")
public interface IGroup extends IDirEntryExtIdSupport, IGroupMember {

	/**
	 * Create group
	 * 
	 * @param uid
	 * @param group
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	void create(@PathParam(value = "uid") String uid, Group group) throws ServerFault;

	/**
	 * Create group with external ID
	 * 
	 * @param uid
	 * @param extId
	 * @param group
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}/{extid}/createwithextid")
	void createWithExtId(@PathParam(value = "uid") String uid, @PathParam(value = "extid") String extId, Group group)
			throws ServerFault;

	/**
	 * Update group
	 * 
	 * @param uid
	 * @param group
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	void update(@PathParam(value = "uid") String uid, Group group) throws ServerFault;

	/**
	 * Touch group (update direntry, vcard etc..)
	 * 
	 * @param uid
	 * @param group
	 * @param updateVcards: should we update the group vcards (default: true)
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/_touch")
	void touch(@PathParam(value = "uid") String uid, @QueryParam(value = "update_vcards") boolean updateVcards)
			throws ServerFault;

	/**
	 * Get group from UID
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}")
	Group get(String uid);

	/**
	 * Get group from UID
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/complete")
	ItemValue<Group> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Get group from its email
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("byEmail/{email}")
	ItemValue<Group> byEmail(@PathParam(value = "email") String email) throws ServerFault;

	/**
	 * Get group from its name
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("byName/{name}")
	ItemValue<Group> byName(@PathParam(value = "name") String name) throws ServerFault;

	/**
	 * Delete group
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	TaskRef delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Get group from external ID
	 * 
	 * @param extId
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_extid/{extid}")
	ItemValue<Group> getByExtId(@PathParam(value = "extid") String extId) throws ServerFault;

	// FIXME only one method
	// @POST
	// public void updateMembers(String uid, MembersMutation mutation)
	// add and remove should be "unitary" (one member)
	@PUT
	@Path("{uid}/members")
	void add(@PathParam(value = "uid") String uid, List<Member> members) throws ServerFault;

	@DELETE
	@Path("{uid}/members")
	void remove(@PathParam(value = "uid") String uid, List<Member> members) throws ServerFault;

	/**
	 * Get all group members
	 * 
	 * @param group uid
	 * @return members belonging to this group
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/members")
	List<Member> getMembers(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Get all expanded group members
	 * 
	 * @param group uid
	 * @return members belonging to this group or its sub-groups
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/expandedmembers")
	List<Member> getExpandedMembers(@PathParam(value = "uid") String uid) throws ServerFault;

	// FIXME add a QueryParam to getMembers :
	// public List<Member> getMembers(@PathParam(value = "uid") String uid,
	// @QueryParam("expand") Boolean expand) throws ServerFault;
	/**
	 * Get User type expanded group members
	 * 
	 * @param group uid
	 * @return members of type User belonging to this group or its sub-groups
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/expandedusersmembers")
	List<Member> getExpandedUserMembers(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Get all group parents UID
	 * 
	 * @param uid
	 * @return parents
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/parents")
	List<ItemValue<Group>> getParents(@PathParam(value = "uid") String uid) throws ServerFault;

	@GET
	@Path("_alluids")
	List<String> allUids() throws ServerFault;

	// FIXME: not used, to remove ?
	@POST
	@Path("_rolegroups")
	Set<String> getGroupsWithRoles(List<String> roles) throws ServerFault;

	/**
	 * Search a group
	 * 
	 * @param query group query
	 * @return list of matching groups
	 * @throws ServerFault
	 */
	@POST
	@Path("_search")
	List<ItemValue<Group>> search(GroupSearchQuery query) throws ServerFault;

	/**
	 * @param uid {@link Group} uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/roles")
	Set<String> getRoles(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * @param uid   {@link Group} uid
	 * @param roles
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/roles")
	void setRoles(@PathParam(value = "uid") String uid, Set<String> roles) throws ServerFault;
}
