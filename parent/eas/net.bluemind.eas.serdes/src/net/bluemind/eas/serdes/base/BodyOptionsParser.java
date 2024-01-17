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
package net.bluemind.eas.serdes.base;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.BodyOptions.MIMESupport;
import net.bluemind.eas.dto.base.BodyOptions.MIMETruncation;
import net.bluemind.eas.serdes.sync.BodyPartPreferenceParser;
import net.bluemind.eas.serdes.sync.BodyPreferenceParser;
import net.bluemind.eas.utils.DOMUtils;

public class BodyOptionsParser {

	public BodyOptions fromOptionsElement(Element option) {
		BodyOptions bo = new BodyOptions();
		String mimeSupport = DOMUtils.getElementText(option, "MIMESupport");
		if (mimeSupport != null) {
			bo.mimeSupport = MIMESupport.fromXml(mimeSupport);
		}

		String mimeTruncation = DOMUtils.getElementText(option, "MIMETruncation");
		if (mimeTruncation != null) {
			bo.mimeTruncation = MIMETruncation.fromXml(mimeTruncation);
		}

		NodeList bodyPreferences = option.getElementsByTagName("BodyPreference");
		if (bodyPreferences != null) {
			bo.bodyPrefs = new ArrayList<>(bodyPreferences.getLength());
			BodyPreferenceParser bpp = new BodyPreferenceParser();
			for (int i = 0; i < bodyPreferences.getLength(); i++) {
				Element bodyPreference = (Element) bodyPreferences.item(i);
				bo.bodyPrefs.add(bpp.parse(bodyPreference));
			}
		}

		NodeList bodyPartPreferences = option.getElementsByTagName("BodyPartPreference");
		if (bodyPartPreferences != null) {
			bo.bodyPartPrefs = new ArrayList<>(bodyPartPreferences.getLength());
			BodyPartPreferenceParser bpp = new BodyPartPreferenceParser();
			for (int i = 0; i < bodyPreferences.getLength(); i++) {
				Element bodyPreference = (Element) bodyPreferences.item(i);
				bo.bodyPartPrefs.add(bpp.parse(bodyPreference));
			}
		}

		return bo;
	}

}
