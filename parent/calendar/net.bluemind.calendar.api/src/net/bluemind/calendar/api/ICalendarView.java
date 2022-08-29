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

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;

/**
 * @deprecated use {@link IUserCalendarViews} containerUid can be constructed
 *             with ({@link ICalendarViewUids#userCalendarView}
 *
 */
@BMApi(version = "3")
@Path("/calendars/view/{containerUid}")
@Deprecated
public interface ICalendarView {

	/**
	 * Retrieves a {@link CalendarView}
	 * 
	 * @param uid
	 * @param view
	 * @throws ServerFault
	 * 
	 */
	@GET
	@Path("{uid}")
	public ItemValue<CalendarView> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Creates a {@link CalendarView}
	 * 
	 * @param uid
	 * @param view
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, CalendarView view) throws ServerFault;

	/**
	 * Updates a {@link CalendarView}
	 * 
	 * @param uid
	 * @param view
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, CalendarView view) throws ServerFault;

	/**
	 * Deletes a {@link CalendarView}
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Returns a list of {@link CalendarView}
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_list")
	public ListResult<ItemValue<CalendarView>> list() throws ServerFault;

	/**
	 * Fetch multiple {@link CalendarView}s from theirs uniques uids
	 * 
	 * @param uids
	 * @return {@link List<ItemValue<CalendarView>>}
	 * @throws ServerFault
	 */
	@POST
	@Path("_mget")
	public List<ItemValue<CalendarView>> multipleGet(List<String> uids) throws ServerFault;

	/**
	 * Applies changes (create, update, delete) to a calendar specified by its
	 * <code>containerUid</code>.
	 * 
	 * @param changes
	 * @throws ServerFault
	 */
	@PUT
	@Path("_mupdates")
	public void updates(CalendarViewChanges changes) throws ServerFault;

	/**
	 * Set user the default view. The default view is used when there is no data to
	 * rely on to initialize calendar display.
	 * 
	 * 
	 * @param id Default view item uid
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/_asdefault")
	public void setDefault(@PathParam("uid") String id) throws ServerFault;

}
