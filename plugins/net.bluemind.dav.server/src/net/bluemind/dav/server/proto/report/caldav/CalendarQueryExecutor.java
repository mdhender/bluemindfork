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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import io.vertx.core.http.HttpServerResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.proto.report.caldav.CalendarQueryQuery.Filter;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.fortuna.ical4j.model.Calendar;

public class CalendarQueryExecutor implements IReportExecutor {

	private static final QName root = CDReports.CALENDAR_QUERY;
	private static final Logger logger = LoggerFactory.getLogger(CalendarQueryExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		CalendarQueryQuery cmq = (CalendarQueryQuery) rq;
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(cmq.getPath());

		if (dr.getResType() == ResType.SCHEDULE_INBOX) { // we don't support search requests on the scheduling inbox
			return new CalendarQueryResponse(rq.getPath(), root, Collections.emptyList(), cmq.getProps());
		}

		ContainerDescriptor cd = lc.vStuffContainer(dr);
		VEventQuery query = new VEventQuery();
		query.from = 0;
		for (Filter f : cmq.getToMatch()) {
			f.update(query);
		}
		List<String> extIds = new LinkedList<>();

		if ("calendar".equals(cd.type)) {
			List<ItemValue<VEventSeries>> events = new LinkedList<>();
			try {
				ICalendar calApi = lc.getCore().instance(ICalendar.class, cd.uid);
				ListResult<ItemValue<VEventSeries>> foundEvents = calApi.search(query);
				events.addAll(foundEvents.values);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				events = ImmutableList.of();
			}
			logger.info("Fetched {} event(s) from {} extId", events.size(), extIds.size());
			return new CalendarQueryResponse(rq.getPath(), root, events, cmq.getProps());
		} else if ("todolist".equals(cd.type)) {
			List<ItemValue<VTodo>> events = new LinkedList<>();
			try {
				ITodoList cal = lc.getCore().instance(ITodoList.class, cd.uid);
				for (String uid : extIds) {
					events.add(cal.getComplete(uid));
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				events = ImmutableList.of();
			}
			logger.info("Fetched {} todos(s) from {} extId", events.size(), extIds.size());
			return new CalendarQueryResponse(rq.getPath(), root, events, cmq.getProps(), null);
		} else {
			logger.error("Query unsupported on " + dr.getResType() + " " + dr.getPath());
			return null;
		}

	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		CalendarQueryResponse cmr = (CalendarQueryResponse) rr;

		MultiStatusBuilder msb = new MultiStatusBuilder();
		if (cmr.getEvents() != null) {
			logger.info("Got {} distinct VEvent UIDs", cmr.getEvents().size());
			for (ItemValue<VEventSeries> serie : cmr.getEvents()) {
				String icsPath = cmr.getHref() + serie.uid + ".ics";
				Element propElem = msb.newResponse(icsPath, 200);
				for (QName prop : cmr.getProps()) {
					Element pe = DOMUtils.createElement(propElem, prop.getPrefix() + ":" + prop.getLocalPart());
					switch (prop.getLocalPart()) {
					case "getetag":
						pe.setTextContent(SyncTokens.getEtag(icsPath, serie.version));
						break;
					case "getcontenttype":
						pe.setTextContent("text/calendar;charset=utf-8");
						break;
					}
				}
			}
		} else {
			for (ItemValue<VTodo> ev : cmr.getTodos()) {
				if (ev == null) {
					continue;
				}
				String icsPath = cmr.getHref() + ev.uid + ".ics";
				Element propElem = msb.newResponse(icsPath, 200);
				for (QName prop : cmr.getProps()) {
					Element pe = DOMUtils.createElement(propElem, prop.getPrefix() + ":" + prop.getLocalPart());
					switch (prop.getLocalPart()) {
					case "getetag":
						pe.setTextContent(SyncTokens.getEtag(icsPath, ev.version));
						break;
					case "calendar-data":
						Calendar cal = VEventServiceHelper.initCalendar();
						cal.getComponents().add(VTodoAdapter.adaptTodo(ev.uid, ev.value));
						pe.setTextContent(cal.toString());
						break;
					}
				}
			}
		}
		msb.sendAs(sr);
	}

	@Override
	public QName getKind() {
		return root;
	}

}
