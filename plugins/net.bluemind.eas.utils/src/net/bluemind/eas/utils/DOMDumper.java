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
package net.bluemind.eas.utils;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.config.global.GlobalConfig;

public class DOMDumper {

	private static boolean withData = GlobalConfig.DATA_IN_LOGS;

	/**
	 * Seeing email/cal/contact data is a security issue for some
	 * administrators. Remove data from a copy of the DOM before printing.
	 * 
	 * @param doc
	 */
	public static final void dumpXml(Logger logger, String prefix, Document doc) {

		try (FastByteArrayOutputStream out = new FastByteArrayOutputStream()) {
			Document c = DOMUtils.cloneDOM(doc);

			if (!withData) {
				trim(c, "ApplicationData");
				trim(c, "AirSyncBase:Data");
				trim(c, "Data");
			}

			// always trim Mime data (ComposeMail)
			trim(c, "Mime");

			DOMUtils.serialise(c, out, true);
			logger.info(prefix + out.toString());

		} catch (Exception e) {

		}
	}

	private static void trim(Document c, String tagName) {
		NodeList nl = c.getElementsByTagName(tagName);
		for (int i = 0; i < nl.getLength(); i++) {
			Node e = nl.item(i);
			NodeList children = e.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				e.removeChild(child);
			}
			e.setTextContent("[trimmed_output]");
		}
	}
}
