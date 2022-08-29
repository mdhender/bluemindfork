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
package net.bluemind.resource.api;

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
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

/**
 * Resources API. Resources are used, for example, to create an entity like a
 * vehicle, a meeting room, a video-projector, etc. They can be categorized by
 * type.
 * 
 * Once created, you can invite a resource to a calendar event. It simply means
 * the resource is booked during the time of the event.
 * 
 * Resource are part of a domain (and only one).
 */
@BMApi(version = "3")
@Path("/resources/{domainUid}")
public interface IResources extends IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> {

	/**
	 * Creates a {@link ResourceDescriptor}.
	 * 
	 * @param uid                { @link ResourceDescriptor } unique id
	 * @param resourceDescriptor { {@link ResourceDescriptor }
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String uid, ResourceDescriptor resourceDescriptor) throws ServerFault;

	/**
	 * Modify an existing {@link ResourceDescriptor}.
	 * 
	 * @param uid                { @link ResourceDescriptor } unique id
	 * @param resourceDescriptor updated { {@link ResourceDescriptor }
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String uid, ResourceDescriptor resourceDescriptor) throws ServerFault;

	/**
	 * Delete an existing {@link ResourceDescriptor}.
	 * 
	 * @param uid { @link ResourceDescriptor } unique id
	 * @throws ServerFault standard error object
	 */
	@DELETE
	@Path("{uid}")
	public TaskRef delete(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Fetch an existing {@link ResourceDescriptor} by its unique id.
	 * 
	 * @param uid { @link ResourceDescriptor } unique id
	 * @return {@link ResourceDescriptor}, or null if the {@link ResourceDescriptor}
	 *         does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("{uid}")
	public ResourceDescriptor get(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Fetch a {@link ResourceDescriptor} icon.
	 * 
	 * @param uid { @link ResourceDescriptor } unique id
	 * @return icon binary data (png format) or null if the
	 *         {@link ResourceDescriptor} does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("{uid}/icon")
	@Produces("image/png")
	public byte[] getIcon(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Set a {@link ResourceDescriptor} icon.
	 * 
	 * @param uid  { @link ResourceDescriptor } unique id
	 * @param icon icon binary data (png format)
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}/icon")
	@Consumes("image/png")
	public void setIcon(@PathParam("uid") String uid, byte[] icon) throws ServerFault;

	/**
	 * Fetch an existing {@link ResourceDescriptor} by its email.
	 * 
	 * @param email { @link ResourceDescriptor } email
	 * @return {@link ResourceDescriptor}
	 *         {@link net.bluemind.core.container.api.ItemValue}, or null if the
	 *         {@link ResourceDescriptor} does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("byEmail/{email}")
	public ItemValue<ResourceDescriptor> byEmail(@PathParam("email") String email) throws ServerFault;

	/**
	 * List all {@link ResourceDescriptor} by type.
	 * 
	 * @param typeUid { @link net.bluemind.resource.api.type.ResourceType } unique
	 *                id
	 * @return list of {@link ResourceDescriptor} uids or null if the type does not
	 *         exists or if there are no {@link ResourceDescriptor} matching.
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("byType/{type}")
	public List<String> byType(@PathParam("type") String typeUid) throws ServerFault;

	/**
	 * Compute the transformed template associated to the given resource if any,
	 * then append it to the given <code>eventDescription</code>.
	 * 
	 * @see ResourceTypeDescriptor#templates
	 * @param resourceUid the identifier of {@link ResourceDescriptor}
	 * @param organizer   the organizer of the calendar event
	 * 
	 * @return the modified - or not - <code>eventDescription</code>
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}/addToEventDesc")
	public String addToEventDescription(@PathParam("uid") String resourceUid, EventInfo eventInfo) throws ServerFault;

	/**
	 * Remove the transformed template associated to the given resource from the
	 * given <code>eventDescription</code>.
	 * 
	 * @see ResourceTypeDescriptor#templates
	 * @param resourceUid the identifier of {@link ResourceDescriptor}
	 * @return the modified - or not - <code>eventDescription</code>
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}/removeFromEventDesc")
	public String removeFromEventDescription(@PathParam("uid") String resourceUid, EventInfo eventInfo)
			throws ServerFault;

}
