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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.user.api.User;

public class CalendarServerPrincipalSearchResponse extends ReportResponse {

	private List<ItemValue<User>> users;
	private List<QName> expected;

	public CalendarServerPrincipalSearchResponse(String href, QName kind) {
		super(href, kind);
	}

	public List<ItemValue<User>> getUsers() {
		return users;
	}

	public void setUsers(List<ItemValue<User>> users) {
		this.users = users;
	}

	public List<QName> getExpected() {
		return expected;
	}

	public void setExpected(List<QName> expected) {
		this.expected = expected;
	}

}
