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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
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
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/calendars/{containerUid}")
public interface ICalendar extends IChangelogSupport, ICrudByIdSupport<VEventSeries>, ICountingSupport, ISortingSupport,
		IDataShardSupport {

	/**
	 * Creates a {@link VEvent}
	 * 
	 * @param uid
	 * @param event
	 * @param sendNotifications
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, VEventSeries event,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, VEventSeries value);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	@PUT
	@Path("id/{id}")
	public Ack createById(@PathParam(value = "id") long id, VEventSeries event) throws ServerFault;

	/**
	 * Updates a {@link VEvent}
	 * 
	 * @param uid
	 * @param event
	 * @param sendNotifications
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, VEventSeries event,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Returns a {@link VEvent}
	 * 
	 * @param uid
	 * @return if successful, this method return a {@link VEvent}
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<VEventSeries> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Returns {@link VEventSeries}
	 * 
	 * @param uid {@link VEventSeries#icsUid}
	 * @return if successful, this method return a {@link VEvent}
	 * @throws ServerFault
	 */
	@GET
	@Path("_icsuid/{uid}")
	public List<ItemValue<VEventSeries>> getByIcsUid(@PathParam(value = "uid") String uid) throws ServerFault;

	@GET
	@Path("{id}/completeById")
	ItemValue<VEventSeries> getCompleteById(@PathParam("id") long id);

	/**
	 * Fetch multiple {@link VEvent}s from theirs uniques uids
	 * 
	 * @param uids
	 * @return {@link List<ItemValue<VEvent>>}
	 * @throws ServerFault
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<VEventSeries>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Deletes a {@link VEvent}
	 * 
	 * @param uid
	 * @param sendNotifications
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Touch an {@link VEvent}
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/_touch")
	public void touch(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Applies changes (create, update, delete) to a calendar specified by its
	 * <code>containerUid</code>.
	 * 
	 * @param changes
	 * @throws ServerFault
	 */
	@PUT
	@Path("_mupdates")
	public ContainerUpdatesResult updates(VEventChanges changes) throws ServerFault;

	/**
	 * Returns a {@link ListResult} of {@link ItemValue} of {@link VEvent}
	 * 
	 * @param query
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_search")
	public ListResult<ItemValue<VEventSeries>> search(VEventQuery query) throws ServerFault;

	/**
	 * CLIENT_WIN style
	 * 
	 * @param since
	 * @param changes
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	@Path("_sync")
	public ContainerChangeset<String> sync(@QueryParam("since") Long since, VEventChanges changes) throws ServerFault;

	/**
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_list")
	public ListResult<ItemValue<VEventSeries>> list() throws ServerFault;

	/**
	 * 
	 * Retrieve reminders (events with alarm)
	 * 
	 * @throws ServerFault
	 */
	@POST
	@Path("_remimder")
	public List<Reminder> getReminder(BmDateTime dtalarm) throws ServerFault;

	/**
	 * @throws ServerFault
	 */
	@POST
	@Path("_reset")
	public TaskRef reset() throws ServerFault;

	/**
	 * Returns all the items uid from the container
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_all")
	List<String> all() throws ServerFault;

	@POST
	@Path("_sorted")
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault;

}
