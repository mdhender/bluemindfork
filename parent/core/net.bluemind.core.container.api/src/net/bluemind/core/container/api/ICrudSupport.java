/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.container.model.ItemValue;

public interface ICrudSupport<T> {

	@GET
	@Path("_alluids")
	public List<String> allUids();

	@POST
	@Path("uid/{uid}")
	Ack update(@PathParam("uid") String uid, T value);

	@PUT
	@Path("uid/{uid}")
	Ack create(@PathParam("uid") String uid, T value);

	@DELETE
	@Path("uid/{uid}")
	void delete(@PathParam("uid") String uid);

	@DELETE
	@Path("_deleteAll")
	void deleteAll();

	@GET
	@Path("uid/{uid}")
	ItemValue<T> getComplete(@PathParam("uid") String uid);

	@POST
	@Path("uid/_mget")
	List<ItemValue<T>> multipleGet(List<String> uids);

}
