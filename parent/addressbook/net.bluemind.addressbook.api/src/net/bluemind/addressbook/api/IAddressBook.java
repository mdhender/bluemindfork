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

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;

/**
 * 
 * Addressbooks APIs. BlueMind provides client and server side implementations
 * of this interface.
 * 
 */
@BMApi(version = "3")
@Path("/addressbooks/{containerUid}")
public interface IAddressBook
		extends IChangelogSupport, ICrudByIdSupport<VCard>, ICountingSupport, ISortingSupport, IDataShardSupport {

	/**
	 * List all items from container
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_all")
	public List<String> allUids() throws ServerFault;

	/**
	 * Creates a new {@link VCard} entry.
	 * 
	 * @param uid  uid of the entry
	 * @param card value of the entry
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, VCard card) throws ServerFault;

	@PUT
	@Path("id/{id}")
	Ack createById(@PathParam("id") long id, VCard value);

	@GET
	@Path("id/{id}")
	ItemValue<VCard> getCompleteById(@PathParam("id") long id);

	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, VCard value);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	/**
	 * Modifies an existing {@link VCard} entry.
	 * 
	 * @param uid  uid of the entry
	 * @param card value of the entry
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, VCard card) throws ServerFault;

	/**
	 * Fetch a {@link VCard} from its unique uid
	 * 
	 * @param uid
	 * @return {@link ItemValue<VCard>}
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<VCard> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetch multiple {@link VCard}s from theirs uniques uids
	 * 
	 * @param uids
	 * @return {@link List<ItemValue<VCard>>}
	 * @throws ServerFault
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<VCard>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Fetch a {@link VCardInfo} from its unique uid
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/info")
	public ItemValue<VCardInfo> getInfo(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Delete vcard entry
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * ElasticSearch based vcard search
	 * 
	 * @param query
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_search")
	public ListResult<ItemValue<VCardInfo>> search(VCardQuery query) throws ServerFault;

	/**
	 * Updates multiples entries at once (should be transactional: if one operation
	 * fail, nothing is written)
	 * 
	 * @param changes
	 * @throws ServerFault
	 */
	@PUT
	@Path("_mupdates")
	public ContainerUpdatesResult updates(VCardChanges changes) throws ServerFault;

	/**
	 * CLIENT_WIN style
	 * 
	 * @param since
	 * @param changes
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_sync")
	public ContainerChangeset<String> sync(@QueryParam("since") Long since, VCardChanges changes) throws ServerFault;

	@POST
	@Path("{uid}/photo")
	public void setPhoto(@PathParam("uid") String uid, byte[] photo) throws ServerFault;

	@GET
	@Path("{uid}/photo")
	@Produces("image/png")
	public byte[] getPhoto(@PathParam("uid") String uid) throws ServerFault;

	@DELETE
	@Path("{uid}/photo")
	public void deletePhoto(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * A scaled-down (22px x 22px) version of the photo
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/icon")
	@Produces("image/png")
	public byte[] getIcon(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Copy entries from one AddressBook to another one
	 * 
	 * @param uids
	 * @param descContainerUid
	 * @throws ServerFault
	 */
	@POST
	@Path("_copy/{destContainerUid}")
	public void copy(List<String> uids, @PathParam("destContainerUid") String descContainerUid) throws ServerFault;

	/**
	 * Move entries from one AddressBook to another one
	 * 
	 * @param uids
	 * @param descContainerUid
	 * @throws ServerFault
	 */
	@POST
	@Path("_move/{destContainerUid}")
	public void move(List<String> uids, @PathParam("destContainerUid") String descContainerUid) throws ServerFault;

	/**
	 * @throws ServerFault
	 */
	@POST
	@Path("_reset")
	public void reset() throws ServerFault;

	@POST
	@Path("_sorted")
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault;

}
