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
package net.bluemind.eas.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.eas.timezone.EASTimeZone;
import net.bluemind.eas.timezone.EASTimeZoneHelper;
import net.bluemind.eas.timezone.TimeZoneCodec;
import net.bluemind.eas.utils.DOMUtils;

public class Decoder {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	public String parseDOMString(Element elt, String default_value) {
		if (elt != null) {
			logger.debug("{}: {}", elt.getNodeName(), elt.getTextContent());
			return elt.getTextContent();
		}
		return default_value;
	}

	public String parseDOMString(Element elt) {
		return parseDOMString(elt, null);
	}

	public Date parseDOMDate(Element elt) {
		if (elt != null) {
			return DateHelper.parseDate(elt.getTextContent());
		} else {
			return null;
		}
	}

	public Byte parseDOMByte(Element elt, Byte default_value) {
		if (elt != null) {
			return parseByte(elt.getTextContent());
		}
		return default_value;
	}

	public Byte parseDOMByte(Element elt) {
		return parseDOMByte(elt, null);
	}

	public Integer parseDOMInt(Element elt, Integer default_value) {
		if (elt != null) {
			return Integer.parseInt(elt.getTextContent());
		}
		return default_value;
	}

	public Integer parseDOMInt(Element elt) {
		return parseDOMInt(elt, null);
	}

	public TimeZone parseDOMTimeZone(Element node) {
		return parseDOMTimeZone(node, null);
	}

	public TimeZone parseDOMTimeZone(Element node, TimeZone defaultTZ) {
		if (node != null) {
			return parseTimeZone(node.getTextContent());
		}
		return defaultTZ;
	}

	public TimeZone parseTimeZone(String b64) {
		EASTimeZone easTz = TimeZoneCodec.decode(b64);
		TimeZone tz = EASTimeZoneHelper.from(easTz);
		return tz;

	}

	public ArrayList<String> parseDOMStringCollection(Element node, String elementName,
			ArrayList<String> default_value) {
		if (node != null) {
			return new ArrayList<String>(Arrays.asList(DOMUtils.getTexts(node, elementName)));
		}

		return default_value;
	}

	public ArrayList<String> parseDOMStringCollection(Element node, String elementName) {
		return parseDOMStringCollection(node, elementName, null);
	}

	public byte parseByte(String str) {
		return Byte.parseByte(str);
	}

	public boolean parseBoolean(String str) {
		return Boolean.parseBoolean(str);
	}

	public Boolean parseDOMBoolean(Element elt, Boolean default_value) {
		if (elt != null) {
			return parseBoolean(elt.getTextContent());
		}
		return default_value;
	}

	public Boolean parseDOMBoolean(Element elt) {
		return parseDOMBoolean(elt, null);
	}

	/**
	 * Return an int else -1
	 * 
	 * @param elt
	 * @return int
	 */
	public int parseDOMNoNullInt(Element elt) {
		if (elt == null)
			return -1;

		return Integer.parseInt(elt.getTextContent());
	}

	/**
	 * Return true if 1 else false
	 * 
	 * @param elt
	 * @return
	 */
	public Boolean parseDOMInt2Boolean(Element elt) {
		if (parseDOMNoNullInt(elt) == 1)
			return Boolean.TRUE;
		else
			return Boolean.FALSE;
	}

	public String parseDOMEmail(Element elt) {
		if (elt == null) {
			return null;
		}
		String email = elt.getTextContent();
		if (email.contains("<")) {
			int id = email.indexOf('<');
			int id2 = email.indexOf('>');
			email = email.substring(id + 1, id2);
		}
		return email;
	}
}
