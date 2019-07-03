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
package net.bluemind.eas.impl;

import java.io.IOException;
import java.io.InputStream;

import org.vertx.java.core.Vertx;
import org.w3c.dom.Document;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.wbxml.WbxmlOutput;

public interface Responder {

	public enum ConnectionHeader {
		close("close"), keepAlive("Keep-Alive");
		public String value;

		private ConnectionHeader(String value) {
			this.value = value;
		}
	}

	WbxmlOutput asOutput(ConnectionHeader con);

	WbxmlOutput asOutput();

	void sendResponse(NamespaceMapping ns, Document doc);

	void sendResponse(NamespaceMapping ns, Document doc, ConnectionHeader con);

	void sendResponseFile(String contentType, InputStream file) throws IOException;

	void sendStatus(int statusCode);

	Vertx vertx();

}
