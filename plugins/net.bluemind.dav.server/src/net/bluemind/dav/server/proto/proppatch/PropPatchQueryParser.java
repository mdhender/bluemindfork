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
package net.bluemind.dav.server.proto.proppatch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Throwables;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.xml.DOMUtils;

public class PropPatchQueryParser {

	private static final Logger logger = LoggerFactory.getLogger(PropPatchQueryParser.class);

	public PropPatchQuery parse(DavResource dr, MultiMap headers, Buffer body) {

		if (logger.isDebugEnabled()) {
			for (String hn : headers.names()) {
				logger.debug("{}: {}", hn, headers.get(hn));
			}
			logger.debug("[{}][{} Bytes]\n{}", dr.getPath(), body.length(), body.toString());
		}

		try {
			Element r = DOMUtils.parse(body.toString()).getDocumentElement();
			PropPatchQuery ppq = new PropPatchQuery(dr);
			final Map<QName, Element> toUpdate = new HashMap<>();
			forEach(r, NS.WEBDAV, "set", new ElemHandler() {
				@Override
				public void on(Element set) {
					forEach(set, NS.WEBDAV, "prop", new ElemHandler() {
						@Override
						public void on(Element pe) {
							Element child = (Element) pe.getFirstChild();
							QName qn = QN.qn(child.getNamespaceURI(), child.getLocalName());
							logger.info("toUpdate: {}", qn);
							toUpdate.put(qn, child);
						}
					});
				}
			});
			ppq.setToUpdate(toUpdate);

			final List<QName> toRemove = new LinkedList<>();
			forEach(r, NS.WEBDAV, "remove", new ElemHandler() {
				@Override
				public void on(Element set) {
					forEach(set, NS.WEBDAV, "prop", new ElemHandler() {
						@Override
						public void on(Element pe) {
							Element child = (Element) pe.getFirstChild();
							QName qn = QN.qn(child.getNamespaceURI(), child.getLocalName());
							logger.info("toRemove: {}", qn);
							toRemove.add(qn);
						}
					});
				}
			});
			ppq.setToRemove(toRemove);
			return ppq;
		} catch (ServerFault e) {
			throw Throwables.propagate(e);
		}
	}

	private static interface ElemHandler {
		void on(Element e);
	}

	private void forEach(Element r, String ns, String local, ElemHandler eh) {
		NodeList nl = r.getElementsByTagNameNS(ns, local);
		int len = nl.getLength();
		for (int i = 0; i < len; i++) {
			Element e = (Element) nl.item(i);
			eh.on(e);
		}
	}

}
