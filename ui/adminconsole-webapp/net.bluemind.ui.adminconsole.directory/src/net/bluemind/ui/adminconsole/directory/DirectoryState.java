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
package net.bluemind.ui.adminconsole.directory;

public class DirectoryState {
	private boolean userFilter;
	private boolean groupFilter;
	private boolean resourceFilter;
	private boolean mailshareFilter;
	private boolean abFilter;
	private boolean calendarFilter;
	private boolean externalUserFilter;

	private String search;
	private String domainUid;

	private static DirectoryState state;

	private DirectoryState() {

	}

	public static DirectoryState state(String domainUid) {
		if (state != null && state.domainUid.equals(domainUid)) {
			return state;
		} else {
			state = new DirectoryState();
			state.domainUid = domainUid;
			return state;
		}
	}

	public void updateState(boolean userFilter, boolean groupFilter, boolean resourceFilter, boolean mailshareFilter,
			boolean calendarFilter, boolean abFilter,
			boolean externalUserFilter, String search) {
		this.userFilter = userFilter;
		this.groupFilter = groupFilter;
		this.resourceFilter = resourceFilter;
		this.mailshareFilter = mailshareFilter;
		this.externalUserFilter = externalUserFilter;
	}

	public boolean isUserFilter() {
		return userFilter;
	}

	public void setUserFilter(boolean userFilter) {
		this.userFilter = userFilter;
	}

	public boolean isGroupFilter() {
		return groupFilter;
	}

	public void setGroupFilter(boolean groupFilter) {
		this.groupFilter = groupFilter;
	}

	public boolean isResourceFilter() {
		return resourceFilter;
	}

	public void setResourceFilter(boolean resourceFilter) {
		this.resourceFilter = resourceFilter;
	}

	public boolean isMailshareFilter() {
		return mailshareFilter;
	}

	public void setMailshareFilter(boolean mailshareFilter) {
		this.mailshareFilter = mailshareFilter;
	}

	public boolean isExternalUserFilter() {
		return externalUserFilter;
	}

	public void setExternalUserFilter(boolean externalUserFilter) {
		this.externalUserFilter = externalUserFilter;
	}

	public boolean isAbFilter() {
		return abFilter;
	}

	public void setAbFilter(boolean abFilter) {
		this.abFilter = abFilter;
	}

	public boolean isCalendarFilter() {
		return calendarFilter;
	}

	public void setCalendarFilter(boolean calendarFilter) {
		this.calendarFilter = calendarFilter;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getDomainUid() {
		return domainUid;
	}
}
