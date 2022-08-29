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

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreCrudSupport;

/**
 * Resource types API. Allows you to categorize resources by type.
 * 
 * Resource types are part of a domain (and only one).
 */
@BMApi(version = "3")
@Path("/resources/{domainUid}/type")
public interface IResourceTypes extends IRestoreCrudSupport<ResourceTypeDescriptor> {

	/**
	 * Create a { @link ResourceTypeDescriptor }.
	 * 
	 * @param uid                    { @link ResourceTypeDescriptor } unique id
	 * @param resourceTypeDescriptor { @link ResourceTypeDescriptor }
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("{identifier}")
	public void create(@PathParam("identifier") String uid, ResourceTypeDescriptor resourceTypeDescriptor)
			throws ServerFault;

	/**
	 * Modify an existing { @link ResourceTypeDescriptor }.
	 * 
	 * @param uid                    { @link ResourceTypeDescriptor } unique id
	 * @param resourceTypeDescriptor updated { @link ResourceTypeDescriptor }
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String uid, ResourceTypeDescriptor resourceTypeDescriptor) throws ServerFault;

	/**
	 * Delete an existing { @link ResourceTypeDescriptor }.
	 * 
	 * @param uid { @link ResourceTypeDescriptor } unique id
	 * @throws ServerFault standard error object
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * 
	 * Fetch a { @link ResourceTypeDescriptor }.
	 * 
	 * @param uid { @link ResourceTypeDescriptor } unique id
	 * @return { @link ResourceTypeDescriptor } or null if it does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("{uid}")
	public ResourceTypeDescriptor get(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Fetch all resources types
	 * 
	 * @return list of all { @link ResourceType }
	 * @throws ServerFault standard error object
	 */
	@GET
	public List<ResourceType> getTypes() throws ServerFault;

	/**
	 * Fetch a { @link ResourceTypeDescriptor } icon.
	 * 
	 * @param uid { @link ResourceTypeDescriptor } unique id
	 * @return icon binary data (png format) or null if the { @link
	 *         ResourceTypeDescriptor } does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("{uid}/icon")
	@Produces("image/png")
	public byte[] getIcon(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Set an icon to { @link ResourceTypeDescriptor }.
	 * 
	 * @param uid  { @link ResourceTypeDescriptor } unique id
	 * @param icon icon binary data (png format)
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}/icon")
	@Consumes("image/png")
	public void setIcon(@PathParam("uid") String uid, byte[] icon) throws ServerFault;
}
