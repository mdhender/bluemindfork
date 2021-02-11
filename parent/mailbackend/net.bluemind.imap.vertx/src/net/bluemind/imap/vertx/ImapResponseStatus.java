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
package net.bluemind.imap.vertx;

import java.util.Optional;

public class ImapResponseStatus<T> {

	public static enum Status {
		Ok, No, Bad;

		public static final Status of(String s) {
			char c = s.charAt(0);
			switch (c) {
			case 'O':
				return Ok;
			case 'N':
				return No;
			case 'B':
			default:
				return Bad;
			}
		}
	}

	public Status status;
	public Optional<T> result = Optional.empty();

	public ImapResponseStatus(Status s, T nullable) {
		this.status = s;
		this.result = Optional.ofNullable(nullable);
	}

}
