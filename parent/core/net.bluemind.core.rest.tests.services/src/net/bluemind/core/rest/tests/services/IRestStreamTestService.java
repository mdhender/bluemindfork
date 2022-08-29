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
package net.bluemind.core.rest.tests.services;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;

@Path("/teststream")
@BMApi(version = "3.0")
public interface IRestStreamTestService {

	@PUT
	public String out(Stream stream);

	@GET
	public Stream in();

	@GET
	@Path("inContentType")
	@Produces("application/octet-stream")
	public Stream inContentType(@QueryParam("mime") String mime, @QueryParam("cs") String cs,
			@QueryParam("fn") String fileName);

	@POST
	public Stream inout(Stream stream);

	@PUT
	@Path("noTimeout")
	public String notTimeout(Stream stream);

}
