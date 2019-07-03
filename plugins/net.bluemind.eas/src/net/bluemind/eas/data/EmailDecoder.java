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

import org.w3c.dom.Element;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSEmail;
import net.bluemind.eas.utils.DOMUtils;

public class EmailDecoder extends Decoder implements IDataDecoder {

	@Override
	public IApplicationData decode(BackendSession bs, Element syncData) {
		MSEmail mail = new MSEmail();

		Element read = DOMUtils.getUniqueElement(syncData, "Read");
		if (read != null) {
			mail.setRead(parseDOMInt2Boolean(read));
		} else {
			mail.setRead(null);
		}

		Element flag = DOMUtils.getUniqueElement(syncData, "Flag");
		if (flag != null) {
			Element fs = DOMUtils.getUniqueElement(flag, "Status");
			if (fs != null) {
				int flagStatus = parseDOMInt(fs);
				if (flagStatus == 0) {
					mail.setStarred(false);
				} else {
					mail.setStarred(true);
				}
			} else {
				mail.setStarred(false);
			}
		} else {
			mail.setStarred(null);
		}

		return mail;
	}
}
