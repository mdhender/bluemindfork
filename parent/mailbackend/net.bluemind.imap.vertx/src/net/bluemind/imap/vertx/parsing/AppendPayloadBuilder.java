/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.parsing;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.cmd.AppendResponse;

public class AppendPayloadBuilder implements ResponsePayloadBuilder<AppendResponse> {

	private Status status;
	private AppendResponse resp;

	@Override
	public boolean untagged(Buffer b) {
		return false;
	}

	@Override
	public boolean tagged(String tag, Status st, String msg) {
		this.status = st;
		if (st == Status.Ok) {
			// [APPENDUID 1509377083 6] Completed
			long uid = -1;
			int idx = msg.lastIndexOf(']');
			if (idx > 0) {
				int space = msg.lastIndexOf(' ', idx - 1);
				String uidParsed = msg.substring(space + 1, idx);
				uid = Long.parseLong(uidParsed);
			}
			this.resp = new AppendResponse(uid);
		} else {
			this.resp = new AppendResponse(msg);
		}
		return true;
	}

	@Override
	public ImapResponseStatus<AppendResponse> build() {
		return new ImapResponseStatus<>(status, resp);
	}

}
