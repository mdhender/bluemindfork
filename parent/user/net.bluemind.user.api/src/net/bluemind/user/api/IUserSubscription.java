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
package net.bluemind.user.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.directory.api.DirEntry;

@BMApi(version = "3")
@Path("/users/{domainUid}/subscriptions")
public interface IUserSubscription {

	/**
	 * List subscribed containers
	 * 
	 * @param subject
	 * @param type
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{subject}")
	public List<ContainerSubscriptionDescriptor> listSubscriptions(@PathParam("subject") String subject,
			@QueryParam("type") String type) throws ServerFault;

	/**
	 * Returns of list of subscribers to a given container
	 * 
	 * @param containerUid
	 * @return list of subscribers uid (as in {@link DirEntry#entryUid})
	 */
	@GET
	@Path("_subscribers/{containerUid}")
	public List<String> subscribers(@PathParam("containerUid") String containerUid);

	/**
	 * Subscribe current User to a list of containers
	 * 
	 * @param subject
	 * @param containers
	 * @throws ServerFault
	 */
	@POST
	@Path("{subject}/_subscribe")
	public void subscribe(@PathParam("subject") String subject, List<ContainerSubscription> subscriptions)
			throws ServerFault;

	/**
	 * Unsubscribe current User from a list of containers
	 *
	 * @param subject
	 * @param containers
	 * @throws ServerFault
	 */
	@POST
	@Path("{subject}/_unsubscribe")
	public void unsubscribe(@PathParam("subject") String subject, List<String> containers) throws ServerFault;

}
