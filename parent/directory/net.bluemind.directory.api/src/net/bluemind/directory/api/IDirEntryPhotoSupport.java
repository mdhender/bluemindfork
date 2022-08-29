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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import net.bluemind.core.api.fault.ServerFault;

public interface IDirEntryPhotoSupport {

	/**
	 * Set a photo, needs to be in PNG format
	 * 
	 * @param uid
	 *                  the directory entry uid
	 * @param photo
	 *                  the photo data
	 * @throws ServerFault
	 *                         common error object
	 */
	@POST
	@Path("{uid}/photo")
	@Consumes("image/png")
	public void setPhoto(@PathParam(value = "uid") String uid, byte[] photo) throws ServerFault;

	/**
	 * Retrieve the photo associated to the {@link DirEntry}
	 * 
	 * @param uid
	 *                the directory entry uid
	 * @return the photo data, PNG format
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("{uid}/photo")
	@Produces("image/png")
	public byte[] getPhoto(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Delete the photo associated to the {@link DirEntry}
	 * 
	 * @param uid
	 *                the directory entry uid
	 * @throws ServerFault
	 *                         common error object
	 */
	@DELETE
	@Path("{uid}/photo")
	public void deletePhoto(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Retrieve the icon/avatar associated to the {@link DirEntry}
	 * 
	 * @param uid
	 *                the directory entry uid
	 * @return the icon data, PNG format
	 * @throws ServerFault
	 *                         common error object
	 */
	@GET
	@Path("{uid}/icon")
	@Produces("image/png")
	public byte[] getIcon(@PathParam("uid") String uid) throws ServerFault;

}
