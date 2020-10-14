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

public class SendmailResponse {
	public final int code;
	private final String message;

	public static SendmailResponse success() {
		return new SendmailResponse(200);
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

	@Override
	public String toString() {
		return String.format("%s: %s", code, message);
	}
}
