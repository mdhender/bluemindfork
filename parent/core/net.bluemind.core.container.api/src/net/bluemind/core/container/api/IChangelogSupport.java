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
package net.bluemind.core.container.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemVersion;

@BMApi(version = "3")
public interface IChangelogSupport {

	/**
	 * @param itemUid
	 * @param since
	 * @return if successful, return a {@link ContainerChangelog}
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/_itemchangelog")
	public ItemChangelog itemChangelog(@PathParam("uid") String itemUid, Long since) throws ServerFault;

	/**
	 * 
	 * 
	 * 
	 * @param since
	 * @return if successful, return a {@link ContainerChangelog}
	 * @throws ServerFault
	 */
	@GET
	@Path("_changelog")
	public ContainerChangelog containerChangelog(Long since) throws ServerFault;

	/**
	 * @param since
	 * @return if successful, return a {@link ContainerChangeset}
	 * @throws ServerFault
	 */
	@GET
	@Path("_changeset")
	public ContainerChangeset<String> changeset(@QueryParam("since") Long since) throws ServerFault;

	/**
	 * @param since
	 * @return if successful, return a {@link ContainerChangeset} with internal
	 *         numeric ids
	 * @throws ServerFault
	 */
	@GET
	@Path("_changesetById")
	public ContainerChangeset<Long> changesetById(@QueryParam("since") Long since) throws ServerFault;

	/**
	 * @param since
	 * @param filter to exclude some items (eg. deleted items)
	 * 
	 * @return if successful, return a {@link ContainerChangeset} with internal
	 *         numeric ids matching the given filter
	 * @throws ServerFault
	 */
	@POST
	@Path("_filteredChangesetById")
	public ContainerChangeset<ItemVersion> filteredChangesetById(@QueryParam("since") Long since, ItemFlagFilter filter)
			throws ServerFault;

	@GET
	@Path("_version")
	public long getVersion() throws ServerFault;

}
