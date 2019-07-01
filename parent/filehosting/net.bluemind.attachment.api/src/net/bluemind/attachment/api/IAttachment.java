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
package net.bluemind.attachment.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.RequiredRoles;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

/**
 * API used in conjunction with
 * {@link net.bluemind.filehosting.api.IFileHosting} to share mail attachments
 * by replacing them with a link
 */
@BMApi(version = "3")
@Path("/attachment/{domainUid}")
@RequiredRoles("canRemoteAttach")
public interface IAttachment {

	/**
	 * Share a mail attachment
	 * 
	 * @param name     the filename
	 * @param document {@link net.bluemind.core.api.Stream} of the file data
	 * @return {@link AttachedFile} containg informations about the shared file
	 * @throws ServerFault common error object
	 */
	@PUT
	@Path("{name}/share")
	public AttachedFile share(@PathParam(value = "name") String name, Stream document) throws ServerFault;

	/**
	 * Deactivate a link to a shared file
	 * 
	 * @param url Link to the shared file
	 * @throws ServerFault common error object
	 */
	@DELETE
	@Path("{url}/unshare")
	public void unShare(@PathParam(value = "url") String url) throws ServerFault;

	/**
	 * Retrieves the configuration
	 * 
	 * @return the Filehosting service configuration
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_config")
	public Configuration getConfiguration() throws ServerFault;

}
