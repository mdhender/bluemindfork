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
package net.bluemind.dav.server.proto.mkcalendar;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;

import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.SAXUtils;

public class MkCalQueryParser {

	public MkCalQuery parse(DavResource res, MultiMap headers, Buffer body) {
		MkCalQuery mcq = new MkCalQuery(res);
		MkCalSaxHandler sax = SAXUtils.parse(new MkCalSaxHandler(), body);
		mcq.kind = sax.kind;
		mcq.displayName = sax.displayName;
		DavHeaders.parse(mcq, headers);
		return mcq;
	}
}
