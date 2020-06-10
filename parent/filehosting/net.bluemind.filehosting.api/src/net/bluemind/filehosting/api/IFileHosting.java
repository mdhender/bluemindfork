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
package net.bluemind.filehosting.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.RequiredRoles;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/filehosting/{domainUid}")
public interface IFileHosting {

	/**
	 * Retrieves the configuration
	 * 
	 * @return the Filehosting service configuration
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_config")
	@RequiredRoles(value = { "admin", "canUseFilehosting", "canRemoteAttach" })
	public Configuration getConfiguration() throws ServerFault;

	/**
	 * Lists files and folders. The listing contains only non-recursive items
	 * 
	 * @param path the folder path
	 * @return the files and folders found under this path
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_list")
	@RequiredRoles("canUseFilehosting")
	public List<FileHostingItem> list(@QueryParam(value = "path") String path) throws ServerFault;

	/**
	 * Finds items in the file hosting repository
	 * 
	 * @param query the query. The format of the query is repository dependent
	 * @return all items matching the query
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_find")
	@RequiredRoles("canUseFilehosting")
	public List<FileHostingItem> find(@QueryParam(value = "query") String query) throws ServerFault;

	/**
	 * Checks if a file exists
	 * 
	 * @param path the relative path to the document
	 * @return true if the file exists, false otherwise
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("{path}/_exists")
	@RequiredRoles(value = { "canUseFilehosting", "canRemoteAttach" })
	public boolean exists(@PathParam(value = "path") String path) throws ServerFault;

	/**
	 * Retrieves a document from the file hosting repository
	 * 
	 * @param path the relative path to the document
	 * @return the document data
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("{path}/_content")
	@RequiredRoles(value = { "canUseFilehosting", "canRemoteAttach" })
	public Stream get(@PathParam(value = "path") String path) throws ServerFault;

	/**
	 * Retrieves a public URL to the document in the file hosting repository
	 * 
	 * @param path           the relative path to the document
	 * @param downloadLimit  the number of times the file can be downloaded, <= 0 if
	 *                       unlimited
	 * @param expirationDate a ISO-8601 compliant date, null otherwise
	 * @return the URL pointing to this document
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_share")
	@RequiredRoles(value = { "canUseFilehosting", "canRemoteAttach" })
	public FileHostingPublicLink share(@QueryParam(value = "path") String path,
			@QueryParam(value = "downloadLimit") Integer downloadLimit,
			@QueryParam(value = "expirationDate") String expirationDate) throws ServerFault;

	/**
	 * Remove a public link
	 * 
	 * @param url the share url
	 * @throws ServerFault common error object
	 */
	@DELETE
	@Path("{url}/unshare")
	public void unShare(@PathParam(value = "url") String url) throws ServerFault;

	/**
	 * Update/insert a document
	 * 
	 * @param path     the relative path in the file hosting repository
	 * @param document the document data
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("{path}")
	@RequiredRoles(value = { "canUseFilehosting", "canRemoteAttach" })
	public void store(@PathParam(value = "path") String path, Stream document) throws ServerFault;

	/**
	 * Deletes a document
	 * 
	 * @param path the relative path in the file hosting repository
	 * @throws ServerFault common error object
	 */
	@DELETE
	@Path("{path}")
	@RequiredRoles(value = { "canUseFilehosting", "canRemoteAttach" })
	public void delete(@PathParam(value = "path") String path) throws ServerFault;

	/**
	 * Retrieves informations about the filehosting implementation
	 * 
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_info")
	public FileHostingInfo info() throws ServerFault;

	/**
	 * Retrieves an entity from the file hosting repository
	 * 
	 * @param uid the entity uid
	 * @return the document data
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("{uid}/_complete")
	public FileHostingItem getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Retrieves a document from the file hosting repository by its public uid
	 * 
	 * @param uid the document uid
	 * @return the document data
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("{uid}/_public")
	public Stream getSharedFile(@PathParam(value = "uid") String uid) throws ServerFault;

}
