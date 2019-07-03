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
package net.bluemind.tag.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;

/**
 * This API is used to manage per-user & per-domain lists of {@link Tag}
 * (keyword and color).
 * 
 * The containerUid is obtained from {@link ITagUids#getDefaultUserTags(String)}
 * or can be a domain uid (ie. domain name).
 * 
 */
@Path("/tags/{containerUid}")
@BMApi(version = "3.0")
public interface ITags extends IDataShardSupport {

	/**
	 * Create a new {@link Tag}. Tags can be associated with items and may be used
	 * to categorize items or for searching.
	 * 
	 * @param uid The unique identifier
	 * @param tag {@link Tag} that will be created.
	 * @throws ServerFault If anything goes wrong
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Tag tag);

	/**
	 * Update a {@link Tag}.
	 * 
	 * @param uid The unique identifier
	 * @param tag {@link Tag} that will be created.
	 * @throws ServerFault If anything goes wrong
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, Tag tag);

	/**
	 * Delete a {@link Tag}.
	 * 
	 * @param uid The unique identifier of the {@link Tag}
	 * @param tag {@link Tag} that will be created.
	 * @throws ServerFault If anything goes wrong
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid);

	/**
	 * Fetch a {@link Tag} from its uid.
	 * 
	 * @param uid The unique identifier
	 * @return {@link ItemValue<Tag>}
	 * @throws ServerFault If anything goes wrong
	 */
	@GET
	@Path("{uid}")
	public ItemValue<Tag> getComplete(@PathParam(value = "uid") String uid);

	/**
	 * Fetch multiple {@link Tag}s from their uids.
	 * 
	 * @param uids the unique identifiers to fetch
	 * @return a list of {@link ItemValue<Tag>}
	 * @throws ServerFault If anything goes wrong
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<Tag>> multipleGet(List<String> uids);

	/**
	 * Find all {@link Tag}s. Domain tags are not returned when this method is
	 * invoked on {@link ITagUids#defaultUserTags(String)}.
	 * 
	 * @return list of {@link Tag}s
	 * @throws ServerFault If anything goes wrong
	 */
	@GET
	public List<ItemValue<Tag>> all();

	/**
	 * Update multiple tags at once.
	 * 
	 * @param changes the batch of changes to apply
	 * @throws ServerFault If anything goes wrong
	 */
	@PUT
	@Path("_mupdates")
	public ContainerUpdatesResult updates(TagChanges changes);

	/**
	 * Get all the changes that occurred on the container starting at the given
	 * version.
	 * 
	 * @param since version of first changes to retrieve
	 * @return {@link ContainerChangelog}
	 * @throws ServerFault If anything goes wrong
	 */
	@GET
	@Path("_changelog")
	public ContainerChangelog changelog(@QueryParam("since") Long since);

	/**
	 * {@link ContainerChangeset} of the container starting at given version.
	 * 
	 * @param since version of first change to retrieve
	 * 
	 * @throws ServerFault If anything goes wrong
	 */
	@GET
	@Path("_changeset")
	public ContainerChangeset<String> changeset(@QueryParam("since") Long since);

	/**
	 * List all {@link Tag} uids in the container.
	 * 
	 * @return a list of {@link Tag} uid
	 * @throws ServerFault If anything goes wrong
	 */
	@GET
	@Path("_alluids")
	public List<String> allUids();

}
