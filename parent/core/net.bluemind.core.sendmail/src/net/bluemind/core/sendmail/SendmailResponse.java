/* BEGIN LICENSE
/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
/* BEGIN LICENSE
/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.sendmail;

import org.columba.ristretto.smtp.SMTPResponse;

/*
250 – This SMTP server response simply means everything went well and your message was delivered to the recipient server.
421 – Your message was temporarily deferred by the recipient server. This is usually a result of too many connections in a short timeframe or too many messages.
450 – Your message was not delivered because the other user mailbox was not available. This can happen if the mailbox is locked or is not routable.
451 – This response is sent when the message simply failed. Often times this is not caused by you, but rather because of a far-end server problem.
452 – This kind of response is sent back when there isn’t enough system storage to send the message. Your message is deferred until storage opens up and it can then be delivered.
550 – The message has failed because the other user’s mailbox is unavailable or because the recipient server rejected your message.
551 – The mailbox your message was intended for does not exist on the recipient server.
552 – The mailbox your message was sent to does not have enough storage to accept your message.
553 – You message was not delivered because the name of the mailbox you sent to does not exist.
554 – This is a very vague message failure response that can refer to any number of problems either on your end or with the recipient server.
 */

public class SendmailResponse {
	public final int code;
	private final String message;

	public static SendmailResponse success() {
		return new SendmailResponse(250);
	}

	public int code() {
		return code;
	}

	public static SendmailResponse fail(String message) {
		return new SendmailResponse(554, message);
	}

	private SendmailResponse(int code) {
		this.code = code;
		this.message = "";
	}

	private SendmailResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}

	SendmailResponse(SMTPResponse resp) {
		this.code = resp.getCode();
		this.message = resp.getMessage();
	}

	public boolean isError() {
		return code == 450 || code > 500;
	}

	@Override
	public String toString() {
		return String.format("%s: %s", code, message);
	}
}
