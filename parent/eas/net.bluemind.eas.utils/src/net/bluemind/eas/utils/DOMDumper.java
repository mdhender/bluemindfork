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

	private DOMDumper() {

	}

	/**
	 * Seeing email/cal/contact data is a security issue for some administrators.
	 * Remove data from a copy of the DOM before printing.
	 * 
	 * @param doc
	 */
	public static final void dumpXml(Logger logger, String prefix, Document doc) {

		try (FastByteArrayOutputStream out = new FastByteArrayOutputStream()) {
			Document c = DOMUtils.cloneDOM(doc);

			if (!GlobalConfig.DATA_IN_LOGS) {
				trim(c, "ApplicationData");
				trim(c, "AirSyncBase:Data");
			}

			// always trim Data
			trim(c, "Data");

			// always trim Mime data (ComposeMail)
			trim(c, "Mime");

			// contact picture
			trim(c, "Picture");

			// attachment content
			trim(c, "Content");

			DOMUtils.serialise(c, out, true);
			if (logger.isInfoEnabled()) {
				logger.info("{}{}", prefix, out);
			}

		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
	}

	private static void trim(Document c, String tagName) {
		NodeList nl = c.getElementsByTagName(tagName);
		for (int i = 0; i < nl.getLength(); i++) {
			Node e = nl.item(i);
			int bytes = 0;
			NodeList children = e.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				bytes += calculateBytes(child);
				e.removeChild(child);
			}
			e.setTextContent("[trimmed_output] of " + bytes + " bytes");
		}
	}

	private static int calculateBytes(Node node) {
		int count = 0;

		if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
			count += node.getNodeValue().trim().length();
		}
		NodeList children = node.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			count += calculateBytes(children.item(j));
		}
		return count;
	}
}
