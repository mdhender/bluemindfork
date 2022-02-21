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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	public final String message;
	private List<FailedRecipient> failedRecipients;

	public static SendmailResponse success() {
		return new SendmailResponse(250);
	}

	public int code() {
		return code;
	}

	public static SendmailResponse fail(String message) {
		return SendmailResponse.fail(message, Collections.emptyList());
	}

	public static SendmailResponse fail(String message, List<FailedRecipient> failedRecipients) {
		return new SendmailResponse(554, message, failedRecipients);
	}

	private SendmailResponse(int code) {
		this(code, "");
	}

	private SendmailResponse(int code, String message) {
		this(code, message, new ArrayList<>());
	}

	private SendmailResponse(int code, String message, List<FailedRecipient> failedRecipients) {
		this.failedRecipients = failedRecipients;
		this.code = code;
		this.message = message;
	}

	SendmailResponse(SMTPResponse resp) {
		this(resp.getCode(), resp.getMessage());
	}

	public SendmailResponse(SMTPResponse data, List<FailedRecipient> failedRecipients) {
		this(data);
		this.failedRecipients = failedRecipients;
	}

	public boolean isError() {
		return code == 450 || code > 500;
	}

	public boolean isOk() {
		return code == 250;
	}

	public List<FailedRecipient> getFailedRecipients() {
		return failedRecipients;
	}

	@Override
	public String toString() {
		String res = String.format("%d: %s", code, message);
		if (!failedRecipients.isEmpty()) {
			res = res + "\r\n error for following recipient(s): ";
			for (int i = 0; i < failedRecipients.size(); i++) {
				res = res + "\r\n" + failedRecipients.get(i);
			}
		}
		return res;
	}
}
