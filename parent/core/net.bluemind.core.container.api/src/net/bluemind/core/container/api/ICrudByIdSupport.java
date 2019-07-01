/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;

public interface ICrudByIdSupport<T> {

	@GET
	@Path("{id}/completeById")
	ItemValue<T> getCompleteById(@PathParam("id") long id);

	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, T value);

	@PUT
	@Path("id/{id}")
	Ack createById(@PathParam("id") long id, T value);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	@DELETE
	@Path("_multipleDelete")
	void multipleDeleteById(List<Long> ids) throws ServerFault;

}
