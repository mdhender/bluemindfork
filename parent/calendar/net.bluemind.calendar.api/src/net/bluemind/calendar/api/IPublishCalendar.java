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
package net.bluemind.calendar.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/calendars/publish/{containerUid}")
public interface IPublishCalendar {

	/**
	 * Generate a url for external calendar access
	 * 
	 * @param mode
	 *                 publish mode, private or public
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	@Path("_generate/{mode}")
	public String generateUrl(@PathParam(value = "mode") PublishMode mode) throws ServerFault;

	@GET
	@Path("generated/{mode}")
	public List<String> getGeneratedUrls(@PathParam(value = "mode") PublishMode mode) throws ServerFault;

	@POST
	@Path("_disable")
	public void disableUrl(String url) throws ServerFault;

	@GET
	@Path("{token}")
	public Stream publish(@PathParam(value = "token") String token) throws ServerFault;

}
