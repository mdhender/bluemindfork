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
package net.bluemind.eas.endpoint.tests.bodyoptions;

import org.w3c.dom.Element;

import net.bluemind.eas.dto.base.BodyType;
import net.bluemind.eas.utils.DOMUtils;

public class TruncatedText implements ISyncOptionsProvider {

	private int len;

	public TruncatedText(int len) {
		this.len = len;
	}

	@Override
	public void setSyncOptions(Element options) {
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", BodyType.PlainText.xmlValue());
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", Integer.toString(len));

	}

}
