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
package net.bluemind.dav.server.proto.delete;

import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.todolist.api.ITodoList;

public class DeleteProtocol implements IDavProtocol<DeleteQuery, DeleteResponse> {

	private static final Logger logger = LoggerFactory.getLogger(DeleteProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<DeleteQuery> handler) {
		r.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				DeleteQuery dq = new DeleteQuery(davRes);
				DavHeaders.parse(dq, r.headers());
				handler.handle(dq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, DeleteQuery query, Handler<DeleteResponse> handler) {
		logger.info("execute");
		DeleteResponse resp = new DeleteResponse();
		DavStore ds = new DavStore(lc);
		DavResource dres = ds.from(query.getPath());
		ResType rt = dres.getResType();
		try {
			ContainerDescriptor cd = lc.vStuffContainer(dres);
			if (rt == ResType.VSTUFF_CONTAINER) {
				logger.info("Should delete container " + cd.uid + " " + cd.type);
				if ("calendar".equals(cd.type)) {
					ICalendar calApi = lc.getCore().instance(net.bluemind.calendar.api.ICalendar.class, cd.uid);
					TaskRef tr = calApi.reset();

					ITask taskApi = lc.getCore().instance(ITask.class, tr.id + "");
					TaskStatus ts = null;
					do {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
						ts = taskApi.status();
					} while (!ts.state.ended);

					IContainers contApi = lc.getCore().instance(IContainers.class);
					contApi.delete(cd.uid);
				} else if ("todolist".equals(cd.type)) {
					ITodoList calApi = lc.getCore().instance(ITodoList.class, cd.uid);
					calApi.reset();
					IContainers contApi = lc.getCore().instance(IContainers.class);
					contApi.delete(cd.uid);
				}
			} else if ("calendar".equals(cd.type)) {
				Matcher m = rt.matcher(dres.getPath());
				m.find();
				String veventUid = m.group(3);
				ICalendar calApi = lc.getCore().instance(net.bluemind.calendar.api.ICalendar.class, cd.uid);
				logger.info("Delete {}", veventUid);
				calApi.delete(veventUid, true);
			} else if ("todolist".equals(cd.type)) {
				Matcher m = rt.matcher(dres.getPath());
				m.find();
				String veventUid = m.group(3);
				ITodoList calApi = lc.getCore().instance(ITodoList.class, cd.uid);
				calApi.delete(veventUid);
			} else {
				logger.error("Not supported path " + dres.getPath());
				resp.setStatus(404);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			resp.setStatus(500);
		}

		handler.handle(resp);
	}

	@Override
	public void write(DeleteResponse response, HttpServerResponse sr) {
		sr.setStatusCode(response.getStatus()).end();
	}

}
