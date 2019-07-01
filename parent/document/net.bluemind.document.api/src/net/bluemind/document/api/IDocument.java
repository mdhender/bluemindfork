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
package net.bluemind.document.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * Document APIs.
 *
 */
@BMApi(version = "3")
@Path("/document/{containerUid}/{itemUid}")
public interface IDocument {

	/**
	 * Creates a new {@link Document} entry
	 * 
	 * @param uid
	 * @param doc
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Document doc) throws ServerFault;

	/**
	 * Updates a {@link Document} entry
	 * 
	 * @param uid
	 * @param doc
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, Document doc) throws ServerFault;

	/**
	 * Deletes a {@link Document} entry
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetches a {@link Document} entry
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}")
	public Document fetch(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetches {@link Document} metadata {@link DocumentMetadata}
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/metadata")
	public DocumentMetadata fetchMetadata(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Retrives all {@link Document} metadata {@link DocumentMetadata}
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_list")
	public List<DocumentMetadata> list() throws ServerFault;
}
