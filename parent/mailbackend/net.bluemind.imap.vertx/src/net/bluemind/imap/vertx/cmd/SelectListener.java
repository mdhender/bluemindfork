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
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import net.bluemind.imap.vertx.ImapProtocolListener;

public class SelectListener extends ImapProtocolListener<SelectResponse> {

	private final SelectResponse resp;

	public SelectListener() {
		super(CompletableFuture.completedFuture(new SelectResponse()));
		this.resp = future.join();
	}

	public void onStatusResponse(ByteBuf s) {
		if (s.getByte(0) == '*') {
			resp.untagged.add(s.slice(2, s.readableBytes() - 2).toString(StandardCharsets.US_ASCII));
		}

	}

}
