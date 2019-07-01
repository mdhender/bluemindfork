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
package net.bluemind.eas.serdes.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.base.AirSyncBaseRequest;
import net.bluemind.eas.dto.base.AirSyncBaseRequest.BodyPartPreference;
import net.bluemind.eas.dto.base.BodyType;

public class BodyPartPreferenceParser {
	private static final Logger logger = LoggerFactory.getLogger(BodyPartPreferenceParser.class);

	public AirSyncBaseRequest.BodyPartPreference parse(Element bpElt) {
		BodyPartPreference ret = new BodyPartPreference();

		NodeList childs = bpElt.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element elt = (Element) node;
			switch (elt.getNodeName()) {
			case "Type":
				ret.type = BodyType.getValueOf(Integer.parseInt(elt.getTextContent()));
				break;
			case "TruncationSize":
				ret.truncationSize = Integer.parseInt(elt.getTextContent());
				break;
			case "AllOrNone":
				ret.allOrNone = Boolean.parseBoolean(elt.getTextContent());
				break;
			case "Preview":
				ret.preview = Integer.parseInt(elt.getTextContent());
				break;
			default:
				logger.warn("BodyPreference element {} not supported ", elt.getNodeName());
				break;
			}
		}
		return ret;
	}

}
