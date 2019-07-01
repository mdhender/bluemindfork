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
package net.bluemind.documentfolder.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/document/{containerUid}/")
public interface IDocumentFolder {

	/**
	 * Creates a {@link DocumentFolder} item
	 * 
	 * @param uid
	 * @param name
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, String name) throws ServerFault;

	/**
	 * Rename a {@link DocumentFolder} item
	 * 
	 * @param uid
	 * @param name
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void rename(@PathParam(value = "uid") String uid, String name) throws ServerFault;

	/**
	 * Removes a {@link DocumentFolder} item
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	@GET
	@Path("{uid}")
	public DocumentFolder get(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Lists all {@link DocumentFolder}
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	public ListResult<DocumentFolder> list() throws ServerFault;

}
