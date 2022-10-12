/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.exchange.mapi.lmtp.filter;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.filters.FilterException;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;

public class MapiFilter implements IMessageFilter {

	public static final String X_BM_INTERNAL_ID = "X-Bm-Internal-Id";

	@Override
	public Message filter(LmtpEnvelope env, Message message) throws FilterException {
		if (message.getHeader().getField(X_BM_INTERNAL_ID) != null) {
			message.getHeader().removeFields(X_BM_INTERNAL_ID);
			return message;
		} else {
			return null;
		}
	}

}
