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
package net.bluemind.dav.server.proto.propfind;

import java.util.LinkedHashSet;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;

import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.SAXUtils;

public class PropFindQueryParser {

	private static final Logger logger = LoggerFactory.getLogger(PropFindQueryParser.class);

	public PropFindQuery parse(DavResource dr, MultiMap headers, Buffer body) {
		for (String hn : headers.names()) {
			if (hn.startsWith("De")) {
				logger.info("{}: {}", hn, headers.get(hn));
			}
		}
		logger.info("[{}][{} Bytes]\n{}", dr.getPath(), body.length(), body.toString());

		PropFindQuery pfq;
		if (body.length() > 0) {
			PropFindSaxHandler sax = SAXUtils.parse(new PropFindSaxHandler(), body);
			pfq = new PropFindQuery(dr, sax.getQueried());
			pfq.setAllProps(sax.isAllProps());
		} else {
			pfq = new PropFindQuery(dr, new LinkedHashSet<QName>());
			pfq.setAllProps(true);
		}
		pfq = DavHeaders.parse(pfq, headers);

		return pfq;

	}

}
