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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import net.bluemind.dav.server.DavActivator;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.SAXUtils;

public class ReportQueryParser {

	private static final Logger logger = LoggerFactory.getLogger(ReportQueryParser.class);

	public ReportQuery parse(DavResource res, MultiMap headers, Buffer body) {
		for (String hn : headers.names()) {
			if (hn.startsWith("Content-")) {
				logger.info("{}: {}", hn, headers.get(hn));
			}
		}
		if (DavActivator.devMode) {
			logger.info("[{}]: {}Bytes.\n{}", res.getPath(), body.length(), body.toString());
		} else {
			logger.info("[{}]: {}Bytes.", res.getPath(), body.length());
		}

		ReportSaxHandler sax = SAXUtils.parse(new ReportSaxHandler(res), body);
		ReportQuery rq = sax.getReportQuery();
		rq = DavHeaders.parse(rq, headers);

		return rq;

	}

}
