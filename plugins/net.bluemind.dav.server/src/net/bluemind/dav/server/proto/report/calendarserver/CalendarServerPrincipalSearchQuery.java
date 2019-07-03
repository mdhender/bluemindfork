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
package net.bluemind.dav.server.proto.report.calendarserver;

import java.util.List;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.store.DavResource;

/**
 * An auto-complete request / user search
 */
public class CalendarServerPrincipalSearchQuery extends ReportQuery {

	private String searchToken;
	private int limit;
	private List<QName> expectedResults;
	private PrincipalSearchContext context;

	protected CalendarServerPrincipalSearchQuery(DavResource dr, QName root) {
		super(dr, root);
	}

	public List<QName> getExpectedResults() {
		return expectedResults;
	}

	public void setExpectedResults(List<QName> expectedResults) {
		this.expectedResults = expectedResults;
	}

	public String getSearchToken() {
		return searchToken;
	}

	public void setSearchToken(String searchToken) {
		this.searchToken = searchToken;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public PrincipalSearchContext getContext() {
		return context;
	}

	public void setContext(PrincipalSearchContext context) {
		this.context = context;
	}

}
