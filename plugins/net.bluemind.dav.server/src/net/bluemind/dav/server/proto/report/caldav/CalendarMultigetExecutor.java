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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerResponse;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.fortuna.ical4j.model.Calendar;

public class CalendarMultigetExecutor implements IReportExecutor {

	private static final QName root = CDReports.CALENDAR_MULTIGET;
	private static final Logger logger = LoggerFactory.getLogger(CalendarMultigetExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		CalendarMultigetQuery cmq = (CalendarMultigetQuery) rq;
		List<String> extIds = new ArrayList<>(cmq.getHrefs().size());
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(cmq.getPath());
		ContainerDescriptor cd = lc.vStuffContainer(dr);
		for (String href : cmq.getHrefs()) {
			int lastDot = href.lastIndexOf('.');
			int lastSlash = href.lastIndexOf('/', lastDot);
			String extId = href.substring(lastSlash + 1, lastDot);

			try {
				extIds.add(URLDecoder.decode(extId, "UTF-8"));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		if ("calendar".equals(cd.type)) {
			List<ItemValue<VEventSeries>> events = new LinkedList<>();
			try {
				ICalendar cal = lc.getCore().instance(ICalendar.class, cd.uid);
				events.addAll(cal.multipleGet(extIds));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				events = ImmutableList.of();
			}
			logger.info("Fetched {} event(s) from {} extId", events.size(), extIds.size());
			return new CalendarMultigetResponse(rq.getPath(), root, events, cmq.getProps());
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
			return new CalendarMultigetResponse(rq.getPath(), root, events, cmq.getProps(), null);
		} else {
			logger.error("Multiget unsupported on " + dr.getResType() + " " + dr.getPath());
			return null;
		}

	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		CalendarMultigetResponse cmr = (CalendarMultigetResponse) rr;

		MultiStatusBuilder msb = new MultiStatusBuilder();
		if (cmr.getEvents() != null) {
			for (ItemValue<VEventSeries> serie : cmr.getEvents()) {

				String icsPath = cmr.getHref() + serie.uid + ".ics";
				Element propElem = msb.newResponse(icsPath, 200);
				for (QName prop : cmr.getProps()) {
					Element pe = DOMUtils.createElement(propElem, prop.getPrefix() + ":" + prop.getLocalPart());
					switch (prop.getLocalPart()) {
					case "getetag":
						pe.setTextContent(SyncTokens.getEtag(icsPath, serie.version));
						break;
					case "calendar-data":
						pe.setTextContent(VEventServiceHelper.convertToIcs(serie));
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
