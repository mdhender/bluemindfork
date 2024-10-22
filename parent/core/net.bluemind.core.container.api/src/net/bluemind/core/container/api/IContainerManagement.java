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
import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemDescriptor;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.acl.AccessControlEntry;

/**
 * Management container
 */
@BMApi(version = "3")
@Path("/containers/_manage/{containerUid}")
public interface IContainerManagement {

	/**
	 * store container ACL
	 * 
	 * @param entries acl
	 * @throws ServerFault
	 */
	@PUT
	@Path("_acl")
	public void setAccessControlList(List<AccessControlEntry> entries) throws ServerFault;

	/**
	 * Retrieve container ACL
	 * 
	 * @return List of access control entries
	 * @throws ServerFault
	 */
	@GET
	@Path("_acl")
	public List<AccessControlEntry> getAccessControlList() throws ServerFault;

	/**
	 * Retrieve container descriptor
	 * 
	 * @return {@link ContainerDescriptor}
	 * @throws ServerFault
	 */
	@GET
	@Path("_descriptor")
	public ContainerDescriptor getDescriptor() throws ServerFault;

	/**
	 * Update container descriptor
	 * 
	 * @param descriptor
	 * @throws ServerFault
	 */
	@POST
	@Path("_descriptor")
	public void update(ContainerModifiableDescriptor descriptor) throws ServerFault;

	@GET
	@Path("_subscription")
	public List<String> subscribers() throws ServerFault;

	/**
	 * Get all container items
	 * 
	 * @return {@link ItemDescriptor} list
	 * @throws ServerFault
	 */
	@GET
	@Path("_list")
	public List<ItemDescriptor> getAllItems() throws ServerFault;

	/**
	 * Get all container items matching the given filter
	 * 
	 * @return {@link ItemDescriptor} list
	 * @throws ServerFault
	 */
	@GET
	@Path("_filtered")
	public List<ItemDescriptor> getFilteredItems(ItemFlagFilter filter) throws ServerFault;

	/**
	 * Get container items
	 * 
	 * @param uids
	 * @return {@link ItemDescriptor} list
	 * @throws ServerFault
	 */
	@POST
	@Path("_mget")
	public List<ItemDescriptor> getItems(List<String> uids) throws ServerFault;

	/**
	 * Set container personal settings
	 * 
	 * @param settings
	 * @throws ServerFault
	 */
	@PUT
	@Path("_personalSettings")
	public void setPersonalSettings(Map<String, String> settings) throws ServerFault;

	/**
	 * Set container settings
	 * 
	 * @param settings
	 * @throws ServerFault
	 */
	@PUT
	@Path("_settings")
	public void setSettings(Map<String, String> settings) throws ServerFault;

	/**
	 * Set a container setting, only one key at a time
	 * 
	 * @param key   string: look ContainerSettingsKeys
	 * @param value
	 * @throws ServerFault
	 */
	@PUT
	@Path("_settings/{key}")
	public void setSetting(@PathParam("key") String key, String value) throws ServerFault;

	/**
	 * Get container settings
	 * 
	 * @throws ServerFault
	 */
	@GET
	@Path("_settings")
	public Map<String, String> getSettings() throws ServerFault;

	@PUT
	@Path("{subject}/offlineSync")
	public void allowOfflineSync(@PathParam("subject") String subject) throws ServerFault;

	@DELETE
	@Path("{subject}/offlineSync")
	public void disallowOfflineSync(@PathParam("subject") String subject) throws ServerFault;

	@POST
	@Path("_canAccess")
	public boolean canAccess(List<String> verbsOrRoles) throws ServerFault;

	@GET
	@Path("_itemCount")
	public Count getItemCount() throws ServerFault;

}
