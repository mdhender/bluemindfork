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
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;

@BMApi(version = "3")
@Path("/mapi_folder/{containerUid}")
public interface IMapiFolder extends ICrudByIdSupport<MapiRawMessage>, IChangelogSupport, ICountingSupport,
		ISortingSupport, IDataShardSupport {

	@POST
	@Path("_reset")
	void reset();

	@GET
	@Path("{id}/completeById")
	ItemValue<MapiRawMessage> getCompleteById(@PathParam("id") long id);

	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, MapiRawMessage value);

	@PUT
	@Path("id/{id}")
	Ack createById(@PathParam("id") long id, MapiRawMessage value);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	@GET
	@Path("{uid}/_itemchangelog")
	public ItemChangelog itemChangelog(@PathParam("uid") String itemUid, Long since) throws ServerFault;

	@GET
	@Path("_changelog")
	public ContainerChangelog containerChangelog(Long since) throws ServerFault;

	@GET
	@Path("_changeset")
	public ContainerChangeset<String> changeset(@QueryParam("since") Long since) throws ServerFault;

	@GET
	@Path("_changesetById")
	public ContainerChangeset<Long> changesetById(@QueryParam("since") Long since) throws ServerFault;

	@POST
	@Path("_filteredChangesetById")
	public ContainerChangeset<ItemVersion> filteredChangesetById(@QueryParam("since") Long since, ItemFlagFilter filter)
			throws ServerFault;

	@GET
	@Path("_count")
	public Count count(ItemFlagFilter filter) throws ServerFault;

	@GET
	@Path("_version")
	public long getVersion() throws ServerFault;

}
