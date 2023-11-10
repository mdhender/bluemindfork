/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.lmtp.filters.internal;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.filters.FilterException;
import net.bluemind.delivery.lmtp.filters.ILmtpFilterFactory;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;

public class RemoveMilterHandledHeader implements IMessageFilter {

	public static final String HANDLED = "X-Bm-Milter-Handled";

	public static class Factory implements ILmtpFilterFactory {

		@Override
		public int getPriority() {
			return 0;
		}

		@Override
		public IMessageFilter getEngine() {
			return new RemoveMilterHandledHeader();
		}

	}

	@Override
	public Message filter(LmtpEnvelope env, Message message) throws FilterException {
		if (message.getHeader().getField(HANDLED) != null) {
			message.getHeader().removeFields(HANDLED);
			return message;
		}

		return null;
	}
}
