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
package net.bluemind.dav.server.proto.post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.dav.server.ics.FreeBusy.CalRequest;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.xml.ScheduleResponseBuilder;
import net.bluemind.vertx.common.Body;

public class FreeBusyProtocol implements IDavProtocol<FBQuery, FBResponse> {

	private static final Logger logger = LoggerFactory.getLogger(FreeBusyProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<FBQuery> handler) {
		logger.warn("Post to schedule outbox, freebusy: {}", r.path());
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer ics) {
				logReq(r, ics);
				FBQuery fb = new FBQuery(davRes);
				try {
					fb.setIcs(ics.getBytes());
					DavHeaders.parse(fb, r.headers());
					handler.handle(fb);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					r.response().setStatusCode(500).end();
				}
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, FBQuery fbq, Handler<FBResponse> handler) {
		try {
			List<CalRequest> requests = net.bluemind.dav.server.ics.FreeBusy.parseRequests(fbq.getIcs(), lc);
			Map<String, VFreebusy> infos = new HashMap<>();
			for (CalRequest cr : requests) {
				IVFreebusy fbApi = lc.getCore().instance(IVFreebusy.class, cr.calUid);
				infos.put(cr.calUid, fbApi.get(cr.range));
			}
			FBResponse fbr = new FBResponse(requests, infos);
			handler.handle(fbr);
		} catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	@Override
	public void write(FBResponse fbr, HttpServerResponse sr) {
		ScheduleResponseBuilder srb = new ScheduleResponseBuilder();
		Set<String> calUids = Sets.newHashSet(fbr.getFbRanges().keySet());
		for (Entry<String, VFreebusy> fb : fbr.getFbRanges().entrySet()) {
			// srb.newResponse("mailto:" + fb.getAtt().getEmail(), fb);
			srb.newResponse("urn:uuid:" + fb.getKey(), fb.getValue());
			calUids.remove(fb.getKey());
		}
		for (String calUid : calUids) {
			// srb.newUnknownRecipientResponse("mailto:" + at.getEmail());
			srb.newUnknownRecipientResponse("urn:uuid:" + calUid);
		}
		srb.sendAs(sr);
	}

	private void logReq(final HttpServerRequest r, Buffer body) {
		for (String hn : r.headers().names()) {
			logger.info("{}: {}", hn, r.headers().get(hn));
		}
		if (body != null) {
			logger.info("parse '{}'\n{}", r.path(), body.toString());
		} else {
			logger.info("parse '{}' q:'{}'", r.path(), r.query());
		}
	}

}
