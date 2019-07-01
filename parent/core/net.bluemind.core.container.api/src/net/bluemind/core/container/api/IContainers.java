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

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;

@BMApi(version = "3")
@Path("/containers/_manage")
public interface IContainers {

	/**
	 * Get a container
	 * 
	 * @param uid the containers unique identifier
	 * @return non null description, throws if not found
	 * @throws ServerFault with {@link ErrorCode#NOT_FOUND} if the container does
	 *                     not exist
	 */
	@GET
	@Path("{uid}")
	// FIXME should not throw exception when container doesnt exists
	public ContainerDescriptor get(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Get a container as if it where requested by another user.
	 * 
	 * @param domainUid
	 * @param userUid
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_forUser")
	ContainerDescriptor getForUser(@QueryParam("domainUid") String domainUid, @QueryParam("userUid") String userUid,
			@QueryParam("uid") String uid) throws ServerFault;

	/**
	 * Get a container
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_ifPresent/{uid}")
	// FIXME replace this api with get (should reply null insteadof throwing
	// exception
	public ContainerDescriptor getIfPresent(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Create a container
	 * 
	 * @param uid
	 * @param descriptor
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String uid, ContainerDescriptor descriptor) throws ServerFault;

	/**
	 * Delete a container
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Update a container
	 * 
	 * @param uid
	 * @param descriptor
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String uid, ContainerModifiableDescriptor descriptor) throws ServerFault;

	/**
	 * @param containerQuery
	 * @return List of all "readeable" containers for current User
	 * @throws ServerFault
	 */
	@POST
	@Path("_list")
	public List<ContainerDescriptor> all(ContainerQuery query) throws ServerFault;

	@POST
	@Path("_listLight")
	public List<BaseContainerDescriptor> allLight(ContainerQuery query) throws ServerFault;

	/**
	 * @param containerQuery
	 * @return List of all "readeable" containers for current User
	 * @throws ServerFault
	 */
	@POST
	@Path("_listforuser")
	public List<ContainerDescriptor> allForUser(@QueryParam("domainUid") String domainUid,
			@QueryParam("userUid") String userUid, ContainerQuery query) throws ServerFault;

	/**
	 * Get container
	 * 
	 * @param containerIds
	 * @return {@link ContainerDescriptor} list
	 * @throws ServerFault
	 */
	@POST
	@Path("_mget")
	public List<ContainerDescriptor> getContainers(List<String> containerIds) throws ServerFault;

	/**
	 * Get container
	 * 
	 * @param containerIds
	 * @return {@link BaseContainerDescriptor} list
	 * @throws ServerFault
	 */
	@POST
	@Path("_mgetLight")
	public List<BaseContainerDescriptor> getContainersLight(List<String> containerIds) throws ServerFault;

	@POST
	@Path("{uid}/_acl")
	public void setAccessControlList(@PathParam("uid") String uid, List<AccessControlEntry> entries) throws ServerFault;

}
