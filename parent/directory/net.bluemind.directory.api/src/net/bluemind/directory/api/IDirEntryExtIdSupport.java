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

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.fault.ServerFault;

/**
 * Manages external IDs
 */
public interface IDirEntryExtIdSupport {

	/**
	 * Adds an external id to a {@link DirEntry}
	 * 
	 * @param uid
	 *                  the {@link DirEntryQuery}'s UID
	 * @param extId
	 *                  the external id
	 * @throws ServerFault
	 *                         common error object
	 */
	@POST
	@Path("{uid}/_extId")
	public void setExtId(@PathParam(value = "uid") String uid, String extId) throws ServerFault;

}
