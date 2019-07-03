/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.client;

public class Request {

	private String cmd;

	private Request(String cmd) {
		this.cmd = cmd;
	}

	public String cmd() {
		return cmd;
	}

	public static Request lhlo(String host) {
		return new Request("LHLO " + host);
	}

	public static Request mailFrom(String email) {
		return new Request("MAIL FROM:<" + email + ">");
	}

	public static Request rcptTo(String email) {
		return new Request("RCPT TO:<" + email + ">");
	}

}
