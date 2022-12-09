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
package net.bluemind.dav.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.dav.server.proto.delete.DeleteProtocol;
import net.bluemind.dav.server.proto.get.GetIcsProtocol;
import net.bluemind.dav.server.proto.get.GetVcfProtocol;
import net.bluemind.dav.server.proto.head.EventDropboxHeadProtocol;
import net.bluemind.dav.server.proto.mkcalendar.MkCalendarProtocol;
import net.bluemind.dav.server.proto.mkcol.CreateEventDropboxProtocol;
import net.bluemind.dav.server.proto.move.MoveProtocol;
import net.bluemind.dav.server.proto.options.OptionsProtocol;
import net.bluemind.dav.server.proto.post.BookMultiputProtocol;
import net.bluemind.dav.server.proto.post.FreeBusyProtocol;
import net.bluemind.dav.server.proto.post.PushProtocol;
import net.bluemind.dav.server.proto.post.VEventStuffPostProtocol;
import net.bluemind.dav.server.proto.propfind.PropFindProtocol;
import net.bluemind.dav.server.proto.proppatch.PropPatchProtocol;
import net.bluemind.dav.server.proto.put.PutProtocol;
import net.bluemind.dav.server.proto.report.ReportProtocol;
import net.bluemind.dav.server.routing.MethodRouter;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.vertx.common.http.BasicAuthHandler;

public final class DavRouter implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(DavRouter.class);

	private final BasicAuthHandler auth;
	public static final String CAL_REDIR = "/.well-known/caldav";
	public static final String CARD_REDIR = "/.well-known/carddav";

	private static Pattern preV35Url = Pattern.compile(Proxy.path + "/principals/__uids__/([0-9]+)/");

	public DavRouter(Vertx vertx) {
		MethodRouter mr = new MethodRouter();
		ResType[] types = ResType.values();
		for (ResType rt : types) {
			mr.propfindHandler(rt, new PropFindProtocol());
		}

		for (ResType rt : types) {
			mr.proppatchHandler(rt, new PropPatchProtocol());
		}

		for (ResType rt : types) {
			mr.optionsHandler(rt, new OptionsProtocol());
		}
		for (ResType rt : types) {
			mr.reportHandler(rt, new ReportProtocol());
		}

		mr.postHandler(ResType.SCHEDULE_OUTBOX, new FreeBusyProtocol());
		mr.postHandler(ResType.VSTUFF_CONTAINER, new VEventStuffPostProtocol());
		mr.postHandler(ResType.APNS, new PushProtocol());
		mr.postHandler(ResType.VCARDS_CONTAINER, new BookMultiputProtocol());

		mr.getHandler(ResType.VSTUFF, new GetIcsProtocol());
		mr.getHandler(ResType.VCARD, new GetVcfProtocol());

		mr.headHandler(ResType.VEVENT_DROPBOX, new EventDropboxHeadProtocol());

		for (ResType rt : types) {
			mr.putHandler(rt, new PutProtocol());
		}

		for (ResType rt : types) {
			mr.deleteHandler(rt, new DeleteProtocol());
		}

		mr.mkcolHandler(ResType.VEVENT_DROPBOX, new CreateEventDropboxProtocol());

		mr.mkcalendarHandler(ResType.VSTUFF_CONTAINER, new MkCalendarProtocol());

		mr.moveHandler(ResType.VSTUFF, new MoveProtocol());

		this.auth = new BasicAuthHandler(vertx, "dav", mr);
	}

	@Override
	public void handle(final HttpServerRequest r) {
		r.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable event) {
				logger.error("Request error: " + event.getMessage(), event);
				r.response().setStatusCode(500).end();
			}
		});
		String p = r.path();
		String ua = r.headers().get("User-Agent");
		if (ua != null && ua.contains("Lightning/")) {
			logger.warn("Preventing Lightning sync with bad request ({}).", ua);
			r.response().setStatusCode(400).setStatusMessage("Lightning is not supported").end();
			return;
		}
		logger.info("{} {} ...", r.method(), p);
		Matcher preMigration = preV35Url.matcher(p);
		if (preMigration.matches()) {
			String newUrl = Proxy.path + "/principals/__uids__/user_entity_" + preMigration.group(1) + "/";
			logger.info("Pre-V3.5 url used {}, redirecting to {}", p, newUrl);
			r.response().putHeader("Location", newUrl).setStatusCode(301).end();
		} else if (CAL_REDIR.equals(p) || CARD_REDIR.equals(p) || !p.startsWith(Proxy.path)) {
			logger.info("*** redirecting '{}' to '{}'", p, Proxy.path);
			r.response().putHeader("Location", Proxy.path).setStatusCode(301).end();
		} else {
			auth.handle(r);
		}

	}
}
