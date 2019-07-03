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
package net.bluemind.im.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/im")
public interface IInstantMessaging {

	@POST
	@Path("_setRoster")
	public void setRoster(@QueryParam("jabberId") String jabberId, String data) throws ServerFault;

	@GET
	@Path("_getRoster")
	public String getRoster(@QueryParam("jabberId") String jabberId) throws ServerFault;

	@POST
	@Path("_getLastMessagesBetween")
	public List<IMMessage> getLastMessagesBetween(@QueryParam("user1") String user1, @QueryParam("user2") String user2,
			@QueryParam("messagesCount") Integer messagesCount) throws ServerFault;

	@POST
	@Path("_sendGroupChatHistory/{groupChatId}")
	public void sendGroupChatHistory(@QueryParam("sender") String sender, @PathParam("groupChatId") String groupChatId,
			List<String> recipients) throws ServerFault;

	@GET
	@Path("_activeUser/{uid}")
	public boolean isActiveUser(@PathParam("uid") String uid) throws ServerFault;

}
