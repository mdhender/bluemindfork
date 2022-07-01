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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.task.api.TaskRef;

/** Calendar operations. */
@BMApi(version = "3", genericType = VEventSeries.class)
@Path("/calendars/{containerUid}")
public interface ICalendar extends IChangelogSupport, ICrudByIdSupport<VEventSeries>, ICountingSupport, ISortingSupport,
		IDataShardSupport, IRestoreItemCrudSupport<VEventSeries> {

	/**
	 * Creates a {@link VEvent}.
	 * 
	 * @param uid               the unique identifier for the new event
	 * @param event             the {@link VEventSeries} to store
	 * @param sendNotifications if <code>true</code> then notify this event creation
	 *                          on the events bus
	 */
	@PUT
	@Path("{uid}")
	void create(@PathParam(value = "uid") String uid, VEventSeries event,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Updates a {@link VEventSeries}.
	 * 
	 * @param uid               the unique identifier of the event
	 * @param event             the {@link VEventSeries} to update
	 * @param sendNotifications if <code>true</code> then notify this event creation
	 *                          on the events bus
	 */
	@POST
	@Path("{uid}")
	void update(@PathParam(value = "uid") String uid, VEventSeries event,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Returns the {@link VEventSeries} identified by the given unique identifier.
	 * 
	 * @param uid the unique identifier of the event
	 * @return a {@link VEventSeries} if successful
	 */
	@GET
	@Path("{uid}/complete")
	ItemValue<VEventSeries> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Returns all {@link VEventSeries} matching the given ICS unique identifier.
	 * 
	 * @param uid the ICS unique identifier
	 * @return the list of matching {@link VEventSeries}
	 */
	@GET
	@Path("_icsuid/{uid}")
	List<ItemValue<VEventSeries>> getByIcsUid(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetch multiple {@link VEventSeries} identified by the given unique
	 * identifiers.
	 * 
	 * @param uids the list of unique identifiers
	 * @return all matching {@link VEventSeries}
	 */
	@POST
	@Path("_mget")
	List<ItemValue<VEventSeries>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Deletes the {@link VEventSeries} identified by the given unique identifier.
	 * 
	 * @param uid               the unique identifier of the event
	 * @param sendNotifications if <code>true</code> then notify this event deletion
	 *                          on the events bus
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	void delete(@PathParam(value = "uid") String uid,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Touch a {@link VEvent}.
	 * 
	 * @param uid the unique identifier of the event
	 */
	@POST
	@Path("{uid}/_touch")
	void touch(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Applies changes (create, update, delete) to a calendar specified by its
	 * <code>containerUid</code>.
	 * 
	 * @param changes the changes to apply
	 */
	@PUT
	@Path("_mupdates")
	ContainerUpdatesResult updates(VEventChanges changes) throws ServerFault;

	/**
	 * Search for events matching the given query.
	 * 
	 * @param query the {@link VEventQuery} to match against
	 * @return the matching {@link VEventSeries}
	 */
	@POST
	@Path("_search")
	ListResult<ItemValue<VEventSeries>> search(VEventQuery query) throws ServerFault;

	/**
	 * Apply the given changes and return the differences since the given time.
	 * CLIENT_WIN style.
	 * 
	 * @param since   the time from wich compare changes
	 * @param changes the changes to apply
	 * @return the {@link ContainerChangeset} of the difSortDescriptorferences
	 */
	@PUT
	@Path("_sync")
	ContainerChangeset<String> sync(@QueryParam("since") Long since, VEventChanges changes) throws ServerFault;

	/**
	 * List all the events of this calendar.
	 * 
	 * @return all the {@link VEventSeries}
	 */
	@GET
	@Path("_list")
	ListResult<ItemValue<VEventSeries>> list() throws ServerFault;

	/**
	 * Remove all events from this calendar.
	 * 
	 * @return the reference to this asynchronous operation
	 */
	@POST
	@Path("_reset")
	TaskRef reset() throws ServerFault;

	/**
	 * Returns all the items uid from the container.
	 * 
	 * @return all the items uid from the container.
	 */
	@GET
	@Path("_all")
	List<String> all() throws ServerFault;

	/**
	 * Check the automatic synchronization is activated for this calendar.
	 * 
	 * @return <code>true</code> if this calendar is automatically synchronized,
	 *         <code>false</code> otherwise
	 */
	@GET
	@Path("_isAutoSyncActivated")
	boolean isAutoSyncActivated() throws ServerFault;

	/**
	 * Search pending counters of the current user
	 * 
	 * @return
	 */
	@GET
	@Path("_search_counters")
	ListResult<ItemValue<VEventSeries>> searchPendingCounters();
}
