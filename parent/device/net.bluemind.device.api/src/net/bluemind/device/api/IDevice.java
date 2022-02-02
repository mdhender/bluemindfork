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
package net.bluemind.device.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3")
@Path("/devices/{userUid}")
public interface IDevice extends IRestoreCrudSupport<Device> {

	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Device device) throws ServerFault;

	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, Device device) throws ServerFault;

	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	@DELETE
	@Path("_deleteAll")
	public void deleteAll() throws ServerFault;

	@GET
	@Path("{uid}/complete")
	public ItemValue<Device> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	@GET
	@Path("{identifier}/byIdentifier")
	public ItemValue<Device> byIdentifier(@PathParam(value = "identifier") String uid) throws ServerFault;

	@GET
	@Path("_list")
	public ListResult<ItemValue<Device>> list() throws ServerFault;

	@POST
	@Path("_wipe/{uid}")
	public void wipe(@PathParam(value = "uid") String uid) throws ServerFault;

	@POST
	@Path("_unwipe/{uid}")
	public void unwipe(@PathParam(value = "uid") String uid) throws ServerFault;

	@PUT
	@Path("{uid}/_partnership")
	public void setPartnership(@PathParam(value = "uid") String uid) throws ServerFault;

	@DELETE
	@Path("{uid}/_partnership")
	public void unsetPartnership(@PathParam(value = "uid") String uid) throws ServerFault;

	@POST
	@Path("{uid}/_lastSync")
	public void updateLastSync(@PathParam(value = "uid") String uid) throws ServerFault;

}
