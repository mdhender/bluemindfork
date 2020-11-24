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
package net.bluemind.domain.api;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;

/**
 * 
 * Manage domain.
 *
 */
@BMApi(version = "3")
@Path("/domains")
public interface IDomains {

	/**
	 * Creates a new {@link Domain}.
	 * 
	 * @param {@link Domain}'s unique id
	 * @param domain {@link Domain}
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String uid, Domain domain) throws ServerFault;

	/**
	 * Modify an existing {@link Domain}.
	 * 
	 * @param uid    {@link Domain}'s unique id
	 * @param domain updated {@link Domain}
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String uid, Domain domain) throws ServerFault;

	/**
	 * Delete the {@link Domain} item itself. You must call first deleteDomainItems
	 * to delete domain's data. This operation is forbidden if there are still
	 * linked data.
	 * 
	 * @param uid {@link Domain}'s unique id
	 * @throws ServerFault standard error object
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * 
	 * Delete data linked to the {@link Domain}. You must call this operation before
	 * deleting the domain itself. According to the data quantity this operation can
	 * be very long, you can follow its progression with the returned
	 * {@link net.bluemind.core.task.api.TaskRef}.
	 * 
	 * @param uid {@link Domain}'s unique id
	 * @return a {@link net.bluemind.core.task.api.TaskRef} to track operation
	 *         progress
	 * @throws ServerFault standard error object
	 */
	@DELETE
	@Path("{uid}/_items")
	public TaskRef deleteDomainItems(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Fetch a {@link Domain} by its uid.
	 * 
	 * @param uid {@link Domain}'s unique id
	 * @return {@link Domain} {@link net.bluemind.core.container.api.ItemValue}, or
	 *         null if the {@link Domain} does not exist
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("{uid}")
	public ItemValue<Domain> get(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Fetch all domains.
	 * 
	 * @return a list of all {@link Domain}
	 *         {@link net.bluemind.core.container.api.ItemValue} managed by the
	 *         server
	 * @throws ServerFault standard error object
	 */
	@GET
	public List<ItemValue<Domain>> all() throws ServerFault;

	// FIXME: add a setAlias operation to avoid overwrite existing aliases
	/**
	 * 
	 * Define domain aliases. If your domain already has aliases, you need to fetch
	 * them first and then call this setAliases operation with all desired aliases.
	 * 
	 * @param uid     {@link Domain}'s unique id
	 * @param aliases set of aliases
	 * @return a {@link net.bluemind.core.task.api.TaskRef} to track operation
	 *         progress
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}/_aliases")
	public TaskRef setAliases(@PathParam("uid") String uid, Set<String> aliases) throws ServerFault;

	/**
	 * Define the domain default alias. The default alias is the name which appears
	 * in the adminconsole when adding/modifying a domain "name". The default alias
	 * is used as the default domain name to use when creating a new email account.
	 * 
	 * The default alias must be contained within the domain aliases.
	 * 
	 * @param uid          {@link Domain}'s unique id
	 * @param defaultAlias default domain name
	 * @throws ServerFault standard error object
	 */
	@POST
	@Path("{uid}/_default_alias")
	public void setDefaultAlias(@PathParam("uid") String uid, String defaultAlias) throws ServerFault;

	/**
	 * Fetch a {@link Domain} by its name or one of its alias.
	 * 
	 * @param name {@link Domain}'s name or alias
	 * @return {@link Domain} {@link net.bluemind.core.container.api.ItemValue}, or
	 *         null if no {@link Domain} match
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("_lookup")
	public ItemValue<Domain> findByNameOrAliases(@QueryParam("name") String name) throws ServerFault;

	@POST
	@Path("{uid}/roles")
	public void setRoles(@PathParam(value = "uid") String uid, Set<String> roles) throws ServerFault;

	@GET
	@Path("{uid}/roles")
	public Set<String> getRoles(@PathParam(value = "uid") String uid) throws ServerFault;

}
