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
package net.bluemind.dav.server.proto.report.caldav;

import java.util.List;

import javax.xml.namespace.QName;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.todolist.api.VTodo;

public class CalendarQueryResponse extends ReportResponse {

	private List<ItemValue<VEventSeries>> events;
	private List<ItemValue<VTodo>> todos;
	private List<QName> props;

	public CalendarQueryResponse(String href, QName kind, List<ItemValue<VEventSeries>> events, List<QName> props) {
		super(href, kind);
		this.events = events;
		this.props = props;
	}

	public CalendarQueryResponse(String href, QName kind, List<ItemValue<VTodo>> events, List<QName> props,
			Object useless) {
		super(href, kind);
		this.todos = events;
		this.props = props;
	}

	public List<ItemValue<VEventSeries>> getEvents() {
		return events;
	}

	public List<QName> getProps() {
		return props;
	}

	public List<ItemValue<VTodo>> getTodos() {
		return todos;
	}

}
