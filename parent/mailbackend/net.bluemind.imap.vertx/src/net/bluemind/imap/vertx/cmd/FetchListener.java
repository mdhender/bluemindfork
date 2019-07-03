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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.imap.vertx.ImapProtocolListener;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;

public class FetchListener extends ImapProtocolListener<FetchResponse> {

	public FetchListener() {
		super();
	}

	@Override
	public void onTaggedCompletion(Status status, ByteBuf b) {
		if (future.isDone()) {
			return;
		}

		future.complete(new FetchResponse(Unpooled.EMPTY_BUFFER));
	}

	@Override
	public void onStatusResponse(ByteBuf b) {
		// this is ok
	}

	@Override
	public void onBinary(ByteBuf b) {
		future.complete(new FetchResponse(b));
	}

}
