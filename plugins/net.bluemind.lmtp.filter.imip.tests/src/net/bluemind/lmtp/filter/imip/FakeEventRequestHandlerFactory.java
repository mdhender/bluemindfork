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
package net.bluemind.lmtp.filter.imip;

import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.testhelper.FakeSendmail;
import net.bluemind.delivery.lmtp.common.LmtpAddress;

public class FakeEventRequestHandlerFactory {

	public EventRequestHandler create() {
		return create(new FakeSendmail(), null);
	}

	public EventRequestHandler create(ISendmail mailer) {
		return create(mailer, null);
	}

	public EventRequestHandler create(String senderMail) {
		return create(new FakeSendmail(), senderMail);
	}

	public EventRequestHandler create(ISendmail mailer, String sender) {
		return new EventRequestHandler(mailer, null, new LmtpAddress(sender));
	}

}
