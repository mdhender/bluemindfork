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
package net.bluemind.dav.server.proto.get;

import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

public class GetIcsProtocol implements IDavProtocol<GetQuery, GetResponse<?>> {

	private static final Logger logger = LoggerFactory.getLogger(GetIcsProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, DavResource davRes, final Handler<GetQuery> handler) {
		GetQuery pq = new GetQuery(davRes);
		handler.handle(pq);
	}

	@Override
	public void execute(LoggedCore lc, GetQuery query, Handler<GetResponse<?>> handler) {
		GetResponse<Object> gr = new GetResponse<>();
		DavResource dr = query.getResource();
		ContainerDescriptor container = lc.vStuffContainer(dr);

		if (container.type.equals("calendar")) {
			try {
				ICalendar calApi = lc.getCore().instance(ICalendar.class, container.uid);
				Matcher m = dr.getResType().matcher(dr.getPath());
				m.find();
				String eventUid = m.group(3);
				ItemValue<VEventSeries> event = calApi.getComplete(eventUid);
				gr.setValue(event);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				gr.setStatus(500);
			}
		} else if (container.type.equals("todolist")) {
			try {
				ITodoList todolistApi = lc.getCore().instance(ITodoList.class, container.uid);
				Matcher m = dr.getResType().matcher(dr.getPath());
				m.find();
				String eventUid = m.group(3);
				ItemValue<VTodo> task = todolistApi.getComplete(eventUid);
				gr.setValue(task);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				gr.setStatus(500);
			}
		} else {
			logger.error("call getIcs on container {} of type {} ", container.uid, container.type);
			gr.setStatus(500);
		}

		handler.handle(gr);
	}

	@Override
	public void write(GetResponse<?> response, HttpServerResponse sr) {
		Buffer b = Buffer.buffer();
		if (response.getValue() != null) {
			Object r = response.getValue();
			if (r instanceof List<?>) {
				ItemValue<VEventSeries> vevents = (ItemValue<VEventSeries>) response.getValue();
				String ics = VEventServiceHelper.convertToIcs(vevents);
				b.appendString(ics);
				sr.headers().set("Content-Type", "text/calendar; charset=\"utf-8\"");
				sr.headers().set("Content-Length", "" + b.length());
				logger.info("[{} Bytes]:\n{}", b.length(), ics);
			} else {
				ItemValue<VTodo> task = (ItemValue<VTodo>) response.getValue();
				String ics = VTodoAdapter.convertToIcs(task);
				b.appendString(ics);
				sr.headers().set("Content-Type", "text/calendar; charset=\"utf-8\"");
				sr.headers().set("Content-Length", "" + b.length());
				logger.info("[{} Bytes]:\n{}", b.length(), b.toString());
			}
		}
		sr.setStatusCode(response.getStatus()).end(b);
	}

}
