/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3", genericType = ContainerSubscriptionModel.class)
@Path("/containers/_subscriptions/{domainUid}/{ownerUid}")
public interface IOwnerSubscriptions
		extends IChangelogSupport, IDataShardSupport, IReadByIdSupport<ContainerSubscriptionModel> {

	@GET
	@Path("_list")
	public List<ItemValue<ContainerSubscriptionModel>> list() throws ServerFault;

	@GET
	@Path("{uid}/complete")
	ItemValue<ContainerSubscriptionModel> getComplete(@PathParam("uid") String uid);

	@POST
	@Path("_mget")
	List<ItemValue<ContainerSubscriptionModel>> getMultiple(List<String> uids);

	/**
	 * @param since
	 * @return if successful, return a {@link ContainerChangeset} with
	 *         {@link ItemIdentifier}
	 * @throws ServerFault when an error occurs
	 */
	@GET
	@Path("_fullChangesetById")
	public ContainerChangeset<ItemIdentifier> fullChangesetById(@QueryParam("since") Long since);

}
