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
package net.bluemind.dav.server.proto.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.IProtocolFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.vertx.common.Body;

public class ReportProtocol implements IDavProtocol<ReportQuery, ReportResponse> {

	private static final Logger logger = LoggerFactory.getLogger(ReportProtocol.class);

	public static final IProtocolFactory<ReportQuery, ReportResponse> FACTORY = new IProtocolFactory<ReportQuery, ReportResponse>() {

		private final ReportProtocol proto = new ReportProtocol();

		@Override
		public IDavProtocol<ReportQuery, ReportResponse> getProtocol() {
			return proto;
		}

		@Override
		public String getExecutorAddress() {
			return "report.executor";
		}
	};

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<ReportQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer body) {
				ReportQueryParser qp = new ReportQueryParser();
				try {
					ReportQuery pfq = qp.parse(davRes, r.headers(), body);
					handler.handle(pfq);
				} catch (Exception t) {
					logger.error(t.getMessage(), t);
					r.response().setStatusCode(501).end();
				}
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, ReportQuery query, Handler<ReportResponse> handler) {
		IReportExecutor re = ReportExecutors.get(query.getKind());
		logger.info("Running {} with {}", query.getKind(), re);
		ReportResponse resp = re.execute(lc, query);
		handler.handle(resp);
	}

	@Override
	public void write(ReportResponse response, HttpServerResponse sr) {
		IReportExecutor re = ReportExecutors.get(response.getKind());
		re.write(response, sr);
	}

}
