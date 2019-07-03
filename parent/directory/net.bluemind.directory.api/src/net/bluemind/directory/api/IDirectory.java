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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/directory/{domain}")
public interface IDirectory {

	/**
	 * Fetch the root {@link DirEntry}
	 * 
	 * @return {@link DirEntry}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	DirEntry getRoot() throws ServerFault;

	/**
	 * Fetch {@link DirEntry} by its path
	 * 
	 * @param path
	 *                 path of the directory entry <br>
	 *                 <b>domainUid/kind/entryUid</b> to find a particular entry
	 *                 <br>
	 *                 <b>domainUid/kind</b> to find all entries of a particular
	 *                 kind<br>
	 *                 <b>domainUid</b> to find all entries of a particular domain
	 * 
	 * @return {@link DirEntry} or null if not found
	 * @throws ServerFault
	 *                         common error object
	 */
	@POST
	@Path("_entry")
	DirEntry getEntry(String path) throws ServerFault;

	/**
	 * Fetch {@link DirEntry}s by their path
	 * 
	 * @param path
	 *                 path of the directory entries <br>
	 *                 <b>domainUid/kind</b> to find all entries of a particular
	 *                 kind<br>
	 *                 <b>domainUid</b> to find all entries of a particular domain
	 * 
	 * @return {@link DirEntry}s
	 * @throws ServerFault
	 *                         common error object
	 */
	@POST
	@Path("_childs")
	List<DirEntry> getEntries(String path) throws ServerFault;

	/**
	 * Delete {@link DirEntry} by path
	 * 
	 * @param path
	 *                 path of the directory entry
	 *                 (<b>domainUid/kind/entryUid</b>)<br>
	 *                 This action will fail if a shorter form of the path (like
	 *                 <b>domain</b> or <b>domain/kind</b>) is used and returns
	 *                 multiple entries.
	 * @throws ServerFault
	 *                         common error object
	 */
	@DELETE
	@Path("{path}")
	public TaskRef delete(@PathParam(value = "path") String path) throws ServerFault;

	/**
	 * Get {@link DirEntry}'s {@link net.bluemind.addressbook.api.VCard}
	 * 
	 * @param uid
	 *                the entry uid
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("{uid}/_vcard")
	public ItemValue<VCard> getVCard(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Delete a {@link DirEntry} by uid
	 * 
	 * @param uid
	 *                the entry uid
	 * @throws ServerFault
	 *                         common error object
	 */
	@DELETE
	@Path("_byentryuid/{entryUid}")
	public TaskRef deleteByEntryUid(@PathParam(value = "entryUid") String entryUid) throws ServerFault;

	/**
	 * Get the domain's {@link ContainerChangelog}
	 * 
	 * @param since
	 *                  timestamp of the first change we want to retrieve
	 * @return {@link ContainerChangelog}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("_changelog")
	public ContainerChangelog changelog(@QueryParam("since") Long since) throws ServerFault;

	/**
	 * Get the domain's {@link ContainerChangeset}
	 * 
	 * @param since
	 *                  timestamp of the first change we want to retrieve
	 * @return {@link ContainerChangeset}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("_changeset")
	public ContainerChangeset<String> changeset(@QueryParam("since") Long since) throws ServerFault;

	/**
	 * Search {@link DirEntry}s by {@link DirEntryQuery}
	 * 
	 * @param query
	 *                  the {@link DirEntryQuery} search parameters
	 * @return List of matching {@link DirEntry}s
	 * @throws ServerFault
	 *                         common error object
	 */
	@POST
	@Path("_search")
	public ListResult<ItemValue<DirEntry>> search(DirEntryQuery query) throws ServerFault;

	/**
	 * Get {@link DirEntry} by uid
	 * 
	 * @param entryUid
	 *                     the unique {@link DirEntry} uid
	 * @return matching {@link DirEntry} or null if not found
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("entry-uid/{entryUid}")
	public DirEntry findByEntryUid(@PathParam("entryUid") String entryUid) throws ServerFault;

	/**
	 * Get a {@link DirEntry}'s icon
	 * 
	 * @param entryUid
	 *                     the unique {@link DirEntry} uid
	 * @return the image data in PNG format or null, if no icon is associated to the
	 *         {@link DirEntry}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("entry-uid/{entryUid}/icon")
	@Produces("image/png")
	public byte[] getEntryIcon(@PathParam("entryUid") String entryUid) throws ServerFault;

	/**
	 * Get a {@link DirEntry}'s photo
	 * 
	 * @param entryUid
	 *                     the unique {@link DirEntry} uid
	 * @return the image data in PNG format or null, if no photo is associated to
	 *         the {@link DirEntry}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("entry-uid/{entryUid}/photo")
	@Produces("image/png")
	public byte[] getEntryPhoto(@PathParam("entryUid") String entryUid) throws ServerFault;

	/**
	 * Get a {@link DirEntry}'s icon
	 * 
	 * @param path
	 *                 path of the directory entry
	 *                 (<b>domainUid/kind/entryUid</b>)<br>
	 *                 This action will fail if a shorter form of the path (like
	 *                 <b>domain</b> or <b>domain/kind</b>) is used and returns
	 *                 multiple entries.
	 * @return the image data in PNG format or null, if no icon is associated to the
	 *         {@link DirEntry}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("_icon/{path}")
	@Produces("image/png")
	public byte[] getIcon(@PathParam("path") String path) throws ServerFault;

	/**
	 * Get all the roles associated to an {@link DirEntry}
	 * 
	 * @param entryUid
	 *                     the unique {@link DirEntry} uid
	 * @return a set containing all roles associated to a {@link DirEntry}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("entry-uid/{entryUid}/rolesfor_")
	public Set<String> getRolesForDirEntry(@PathParam("entryUid") String entryUid) throws ServerFault;

	/**
	 * Get all the roles associated to an {@link net.bluemind.directory.api.OrgUnit}
	 * 
	 * @param orgUnitUid
	 *                       the unique {@link net.bluemind.directory.api.OrgUnit}'s
	 *                       id
	 * @return a set containing all roles associated to an
	 *         {@link net.bluemind.directory.api.OrgUnit}
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("ou-uid/{ouUid}/rolesfor_")
	public Set<String> getRolesForOrgUnit(@PathParam("ouUid") String orgUnitUid) throws ServerFault;

	/**
	 * Fetch a {@link DirEntry} by its email address
	 * 
	 * @param email
	 *                  the {@link DirEntry}'s email address
	 * @return the matching {@link DirEntry} or null, if not found
	 */
	@GET
	@Path("_byEmail/{email}")
	DirEntry getByEmail(@PathParam("email") String email);

	/**
	 * Fetch a list of {@link net.bluemind.core.container.model}({@link DirEntry})
	 * by their internal numerical ids
	 * 
	 * @param id
	 *               list of internal numerical ids
	 * @return list of matching {@link DirEntry}'s
	 */
	@POST
	@Path("_mget")
	List<ItemValue<DirEntry>> getMultiple(List<String> id);

	/**
	 * Transfer a {@link DirEntry} to a different
	 * {@link net.bluemind.server.api.Server} This transfers all database related
	 * data as well as all emails to the new server. This action can potentially
	 * take a very long time
	 * 
	 * @param entryUid
	 *                      the unique {@link DirEntry} uid
	 * @param serverUid
	 *                      the unique {@link net.bluemind.server.api.Server} uid
	 * @return a {@link net.bluemind.core.task.api.TaskRef} referencing this
	 *         operation
	 * @throws ServerFault
	 *                         common error object
	 */
	@POST
	@Path("_xfer/{entryUid}/{serverUid}")
	TaskRef xfer(@PathParam("entryUid") String entryUid, @PathParam("serverUid") String serverUid) throws ServerFault;

}
