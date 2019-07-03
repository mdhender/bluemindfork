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
package net.bluemind.imap.vertx.cmd;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import net.bluemind.imap.vertx.ImapProtocolListener;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;

public class AppendListener extends ImapProtocolListener<AppendResponse> {

	public AppendListener() {
		super();
	}

	public void onTaggedCompletion(Status status, ByteBuf b) {
		if (future.isDone()) {
			return;
		}
		AppendResponse ar = null;
		String resp = b.toString(StandardCharsets.US_ASCII);
		switch (status) {
		case Ok:
			// [APPENDUID 1509377083 6] Completed
			long uid = -1;
			int idx = resp.lastIndexOf(']');
			if (idx > 0) {
				int space = resp.lastIndexOf(' ', idx - 1);
				String uidParsed = resp.substring(space + 1, idx);
				uid = Long.parseLong(uidParsed);
			}
			ar = new AppendResponse(uid);
			break;
		case No:
		case Bad:
			ar = new AppendResponse(resp);
			break;
		}
		future.complete(ar);
	}

}
