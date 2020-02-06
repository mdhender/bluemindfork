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
package net.bluemind.dav.server.xml;

import java.io.ByteArrayInputStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import io.vertx.core.buffer.Buffer;

public class SAXUtils {

	public static final <T extends ContentHandler> T parse(T handler, Buffer xml) {
		XMLReader sax;
		try {
			sax = XMLReaderFactory.createXMLReader();
			sax.setContentHandler(handler);
			ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
			sax.parse(new InputSource(in));
			return handler;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
