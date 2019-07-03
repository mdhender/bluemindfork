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
package net.bluemind.exchange.mapi.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/mapi_folders/{domainUid}/{mailboxUid}")
public interface IMapiFoldersMgmt {

	@PUT
	void store(MapiFolder mf);

	@GET
	@Path("{containerUid}")
	MapiFolder get(@PathParam("containerUid") String containerUid);

	@POST
	@Path("{containerUid}/_repair")
	void repair(@PathParam("containerUid") String containerUid);

	@DELETE
	@Path("{containerUid}")
	void delete(@PathParam("containerUid") String containerUid);

	@DELETE
	@Path("_delete")
	void deleteAll();

}
