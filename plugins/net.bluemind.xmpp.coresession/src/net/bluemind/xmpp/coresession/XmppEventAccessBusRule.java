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
package net.bluemind.xmpp.coresession;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IEventBusAccessRule;

public class XmppEventAccessBusRule implements IEventBusAccessRule {

	@Override
	public boolean match(String path) {
		return path.startsWith("xmpp/session/") || path.startsWith("xmpp/muc/")
				|| path.equals("xmpp/sessions-manager:open");
	}

	@Override
	public boolean authorize(BmContext context, String path) {
		if (path.equals("xmpp/sessions-manager:open")) {
			return true;
		} else if (path.startsWith("xmpp/session/")) {
			return path.startsWith("xmpp/session/" + context.getSecurityContext().getSessionId());
		} else if (path.startsWith("xmpp/muc/")) {
			return path.startsWith("xmpp/muc/" + context.getSecurityContext().getSessionId());
		} else {
			return false;
		}
	}

}
