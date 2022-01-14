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
package net.bluemind.directory.api;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * Class representing {@link DirEntry} search parameters
 */
@BMApi(version = "3")
public class DirEntryQuery {

	/**
	 * Search direction, ascending by default
	 */
	public Order order = defaultOrder();
	/**
	 * Filter by name
	 */
	public String nameFilter;
	/**
	 * True, if the search should ignore hidden {@link DirEntry}s. Default value is
	 * True
	 */
	public boolean hiddenFilter = true;
	/**
	 * Filter by email
	 */
	public String emailFilter;
	/**
	 * Filter by name or email
	 */
	public String nameOrEmailFilter;
	/**
	 * Filter by State (Archived, Active, All)
	 */
	public StateFilter stateFilter;
	/**
	 * True, if the search should ignore internal(system) {@link DirEntry}s. Default
	 * value is True
	 */
	public boolean systemFilter = true;
	/**
	 * Filter by Kind (USER, GROUP, RESOURCE, MAILSHARE, CALENDAR, ADDRESSBOOK,
	 * DOMAIN, ORG_UNIT, EXTERNALUSER)
	 */
	public List<BaseDirEntry.Kind> kindsFilter;
	/**
	 * Filter by UID
	 */
	public List<String> entries;
	/**
	 * Filter by Account Type
	 */
	public DirEntry.AccountType accountTypeFilter;
	/**
	 * Search offset
	 */
	public int from = 0;
	/**
	 * Maximal result size, -1 if there is no limit. The default value is -1
	 */
	public int size = -1;
	/**
	 * Filter by UID
	 */
	public List<String> entryUidFilter;
	/**
	 * True if the search returns only manageable {@link DirEntry}s (entries where
	 * the executing user owns the role MANAGE)
	 */
	public boolean onlyManagable = false;

	/**
	 * Filter by data location
	 */
	public String dataLocation;

	@BMApi(version = "3")
	public static enum StateFilter {
		Archived, Active, All
	}

	@BMApi(version = "3")
	public static enum OrderBy {
		kind, displayname
	}

	@BMApi(version = "3")
	public static enum Dir {
		asc, desc
	}

	@BMApi(version = "3")
	public static class Order {
		public OrderBy by;
		public Dir dir;
	}

	/**
	 * Creates a query which returns all {@link DirEntry}s
	 * 
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery all() {
		return new DirEntryQuery();
	}

	/**
	 * Creates a query which filters by the given UIDs
	 * 
	 * @param uids List of UIDs
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery entries(List<String> uids) {
		DirEntryQuery q = new DirEntryQuery();
		q.entries = uids;
		return q;
	}

	/**
	 * Creates a query which filters by the given UIDs
	 * 
	 * @param uids Array of UIDs
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery entries(String... uid) {
		DirEntryQuery q = new DirEntryQuery();
		q.entries = Arrays.asList(uid);
		return q;
	}

	/**
	 * Returns the default sort order
	 * 
	 * @return the default sort order
	 */
	public static Order defaultOrder() {
		return order(OrderBy.displayname, Dir.asc);
	}

	/**
	 * Sets the sort order
	 * 
	 * @param by  Defines the property used by the order statement
	 * @param dir The sort order
	 * @return the Order object
	 */
	public static Order order(OrderBy by, Dir dir) {
		Order o = new Order();
		o.by = by;
		o.dir = dir;
		return o;
	}

	/**
	 * Creates a query which filters by the kind parameter
	 * 
	 * @param kinds The requested kinds
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery filterKind(DirEntry.Kind... kinds) {
		DirEntryQuery q = new DirEntryQuery();
		q.kindsFilter = Arrays.asList(kinds);
		return q;
	}

	/**
	 * Creates a query which filters by name
	 * 
	 * @param name The name
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery filterName(String name) {
		DirEntryQuery q = new DirEntryQuery();
		q.nameFilter = name;
		return q;
	}

	/**
	 * Creates a query which filters by email
	 * 
	 * @param email The email address
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery filterEmail(String email) {
		DirEntryQuery q = new DirEntryQuery();
		q.emailFilter = email;
		return q;
	}

	/**
	 * Creates a query which filters by the UID parameter
	 * 
	 * @param entryUids Array of UIDs
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery filterEntryUid(String... entryUids) {
		DirEntryQuery q = new DirEntryQuery();
		q.entryUidFilter = Arrays.asList(entryUids);
		return q;
	}

	/**
	 * Creates a query which filters by name or email
	 * 
	 * @param string search value
	 * @return {@link DirEntryQuery}
	 */
	public static DirEntryQuery filterNameOrEmail(String string) {
		DirEntryQuery q = new DirEntryQuery();
		q.nameOrEmailFilter = string;
		return q;
	}

}
