/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3", internal = true)
@Path("/bmfilehosting")
public interface IInternalBMFileSystem extends IFileHosting {

	@GET
	@Path("_shares")
	public List<String> getShareUidsByPath(String path) throws ServerFault;

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
