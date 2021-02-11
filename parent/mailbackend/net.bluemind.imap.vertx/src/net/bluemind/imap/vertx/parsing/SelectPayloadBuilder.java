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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.cmd.SelectResponse;

public class SelectPayloadBuilder implements ResponsePayloadBuilder<SelectResponse> {

	List<String> untagged = new ArrayList<>();
	private Status status;

	@Override
	public boolean untagged(Buffer b) {
		untagged.add(b.toString(StandardCharsets.US_ASCII));
		return false;
	}

	@Override
	public boolean tagged(String tag, Status st, String msg) {
		this.status = st;
		return true;
	}

	@Override
	public ImapResponseStatus<SelectResponse> build() {
		SelectResponse sr = new SelectResponse();
		sr.untagged = untagged;
		return new ImapResponseStatus<>(status, sr);
	}

}
