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
package net.bluemind.addressbook.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;

/**
 * Addressbooks management api
 *
 */
@BMApi(version = "3")
@Path("/mgmt/addressbooks")
public interface IAddressBooksMgmt {

	@POST
	@Path("_reindex")
	/**
	 * reindex all addressbooks (drop current index and recreate them)
	 * 
	 * @return
	 * @throws ServerFault
	 */
	public TaskRef reindexAll() throws ServerFault;

	@POST
	@Path("_reindexDomain")

	/**
	 * reindex all addressbooks of a domain
	 * 
	 * @return
	 * @throws ServerFault
	 */
	public TaskRef reindexDomain(@QueryParam("domain") String domainUid) throws ServerFault;

	/**
	 * reindex an addressbook
	 * 
	 * @param bookUid
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{containerUid}/_reindex")
	public TaskRef reindex(@PathParam("containerUid") String bookUid) throws ServerFault;

	public static class ChangesetItem {
		public String type;
		public ItemValue<VCard> item;
	}

	@GET
	@Path("{containerUid}/_backupstream")
	public Stream backup(@PathParam("containerUid") String abUid, @QueryParam("since") Long since) throws ServerFault;

	@POST
	@Path("{containerUid}/_restorestream")
	public void restore(@PathParam("containerUid") String abUid, Stream restoreStream,
			@QueryParam("reset") boolean resetBeforeRestore) throws ServerFault;

	@DELETE
	@Path("{containerUid}")
	public void delete(@PathParam("containerUid") String abUid) throws ServerFault;

	@Path("{containerUid}")
	@GET
	public AddressBookDescriptor getComplete(@PathParam("containerUid") String uid) throws ServerFault;

	@Path("{containerUid}")
	@PUT
	public void create(@PathParam("containerUid") String uid, AddressBookDescriptor descriptor,
			@QueryParam("isDefault") boolean isDefault) throws ServerFault;

	@Path("{containerUid}")
	@POST
	public void update(@PathParam("containerUid") String uid, AddressBookDescriptor descriptor) throws ServerFault;
}