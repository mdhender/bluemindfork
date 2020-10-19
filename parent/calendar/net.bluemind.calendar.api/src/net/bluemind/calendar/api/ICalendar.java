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

/** Calendar operations. */
@BMApi(version = "3")
@Path("/calendars/{containerUid}")
public interface ICalendar extends IChangelogSupport, ICrudByIdSupport<VEventSeries>, ICountingSupport, ISortingSupport,
		IDataShardSupport {

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
	public void create(@PathParam(value = "uid") String uid, VEventSeries event,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Update the event identified by the given <i>item identifier</i>.
	 * 
	 * @param id    the <i>item identifier</i> of the {@link VEventSeries}
	 * @param value the new value to set
	 * @return the acknowledgement object with the new version of the updated item
	 */
	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, VEventSeries value);

	/**
	 * Delete the event identified by the given <i>item identifier</i>.
	 * 
	 * @param the <i>item identifier</i> of the {@link VEventSeries}
	 */
	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	/**
	 * Creates a {@link VEventSeries}.
	 * 
	 * @param id    the <i>item identifier</i> of the {@link VEventSeries}
	 * @param event the {@link VEventSeries} to store
	 * @return the acknowledgement object with the new version of the updated item
	 */
	@PUT
	@Path("id/{id}")
	public Ack createById(@PathParam(value = "id") long id, VEventSeries event) throws ServerFault;

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
	public void update(@PathParam(value = "uid") String uid, VEventSeries event,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Returns the {@link VEventSeries} identified by the given unique identifier.
	 * 
	 * @param uid the unique identifier of the event
	 * @return a {@link VEventSeries} if successful
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<VEventSeries> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Returns all {@link VEventSeries} matching the given ICS unique identifier.
	 * 
	 * @param uid the ICS unique identifier
	 * @return the list of matching {@link VEventSeries}
	 */
	@GET
	@Path("_icsuid/{uid}")
	public List<ItemValue<VEventSeries>> getByIcsUid(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Retrieve the {@link VEventSeries} identified by the given identifier.
	 * 
	 * @param id the identifier of the event
	 * @return a {@link VEventSeries} if successfulSortDescriptor
	 */
	@GET
	@Path("{id}/completeById")
	ItemValue<VEventSeries> getCompleteById(@PathParam("id") long id);

	/**
	 * Fetch multiple {@link VEventSeries} identified by the given unique
	 * identifiers.
	 * 
	 * @param uids the list of unique identifiers
	 * @return all matching {@link VEventSeries}
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<VEventSeries>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Fetch multiple {@link VEventSeries} from theirs uniques ids
	 * 
	 * @param ids the list of unique id
	 * @return all matching {@link VEventSeries}
	 */
	@POST
	@Path("_mgetById")
	public List<ItemValue<VEventSeries>> multipleGetById(List<Long> ids) throws ServerFault;

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
	public void delete(@PathParam(value = "uid") String uid,
			@QueryParam(value = "sendNotifications") Boolean sendNotifications) throws ServerFault;

	/**
	 * Touch a {@link VEvent}.
	 * 
	 * @param uid the unique identifier of the event
	 */
	@POST
	@Path("{uid}/_touch")
	public void touch(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Applies changes (create, update, delete) to a calendar specified by its
	 * <code>containerUid</code>.
	 * 
	 * @param changes the changes to apply
	 */
	@PUT
	@Path("_mupdates")
	public ContainerUpdatesResult updates(VEventChanges changes) throws ServerFault;

	/**
	 * Search for events matching the given query.
	 * 
	 * @param query the {@link VEventQuery} to match against
	 * @return the matching {@link VEventSeries}
	 */
	@POST
	@Path("_search")
	public ListResult<ItemValue<VEventSeries>> search(VEventQuery query) throws ServerFault;

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
	public ContainerChangeset<String> sync(@QueryParam("since") Long since, VEventChanges changes) throws ServerFault;

	/**
	 * List all the events of this calendar.
	 * 
	 * @return all the {@link VEventSeries}
	 */
	@GET
	@Path("_list")
	public ListResult<ItemValue<VEventSeries>> list() throws ServerFault;

	/**
	 * Remove all events from this calendar.
	 * 
	 * @return the reference to this asynchronous operation
	 */
	@POST
	@Path("_reset")
	public TaskRef reset() throws ServerFault;

	/**
	 * Returns all the items uid from the container.
	 * 
	 * @return all the items uid from the container.
	 */
	@GET
	@Path("_all")
	List<String> all() throws ServerFault;

	/**
	 * Sort the events item identifiers in function of the given
	 * {@link SortDescriptor}.
	 * 
	 * @param the sort directive
	 * @return the sorted event items identifiers
	 */
	@POST
	@Path("_sorted")
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault;

	/**
	 * Check the automatic synchronization is activated for this calendar.
	 * 
	 * @return <code>true</code> if this calendar is automatically synchronized,
	 *         <code>false</code> otherwise
	 */
	@GET
	@Path("_isAutoSyncActivated")
	public boolean isAutoSyncActivated() throws ServerFault;

}
