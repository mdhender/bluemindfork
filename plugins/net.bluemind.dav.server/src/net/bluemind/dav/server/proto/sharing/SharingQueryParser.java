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
package net.bluemind.dav.server.proto.sharing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;

import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.SAXUtils;

public class SharingQueryParser {
	private static final Logger logger = LoggerFactory.getLogger(SharingQueryParser.class);

	public SharingQuery parse(DavResource res, MultiMap headers, Buffer body) {
		for (String hn : headers.names()) {
			logger.info("{}: {}", hn, headers.get(hn));
		}
		logger.info("[{}][{} Bytes]\n{}", res.getPath(), body.length(), body.toString());

		SharingQuery sr = new SharingQuery(res);
		SharingQuerySaxHandler sax = SAXUtils.parse(new SharingQuerySaxHandler(), body);
		sr.setSharings(sax.getSharings());
		DavHeaders.parse(sr, headers);
		return sr;
	}

}
