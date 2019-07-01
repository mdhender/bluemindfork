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
package net.bluemind.dav.server.proto.report.webdav;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.LoggedCore;

public class PrincipalSearchPropertySetExecutor implements IReportExecutor {

	private static final QName root = new QName(NS.WEBDAV, "principal-search-property-set");

	private static final Logger logger = LoggerFactory.getLogger(PrincipalSearchPropertySetExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		return new PrincipalSearchPropertySetResponse(rq.getPath(), root);
	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		String f = Proxy.staticDataPath + "/report_principal-search-property-set.xml";
		logger.info("\n=============== SENDFILE {}, WOOT WOOT WOOT ===============\n", f);
		// according to wireshark, apple server adds no content type
		// sr.headers().set("Content-Type", "text/xml");
		sr.sendFile(f);
	}

	@Override
	public QName getKind() {
		return root;
	}

}
