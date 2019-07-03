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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;

/**
 * Grants read access to a {@link ContainerChangeset} of the containers owned by
 * a given ownerUid
 *
 */
@BMApi(version = "3")
@Path("/containers/_hierarchy/{domainUid}/{ownerUid}")
public interface IContainersFlatHierarchy extends IChangelogSupport, IDataShardSupport {

	@GET
	@Path("_list")
	public List<ItemValue<ContainerHierarchyNode>> list() throws ServerFault;

	@GET
	@Path("{uid}/complete")
	ItemValue<ContainerHierarchyNode> getComplete(@PathParam("uid") String uid);

	@GET
	@Path("{id}/completeById")
	ItemValue<ContainerHierarchyNode> getCompleteById(@PathParam("id") long id);

	@GET
	@Path("_mgetById")
	List<ItemValue<ContainerHierarchyNode>> getMultipleById(List<Long> id);

}
