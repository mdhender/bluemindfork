/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.vertx.impl.postfix;

import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import net.bluemind.central.reverse.proxy.model.client.PostfixMapsStoreClient;

public class PostfixMapsHandler implements Handler<Buffer> {
	private static final Logger logger = LoggerFactory.getLogger(PostfixMapsHandler.class);

	private enum ResponseCode {
		OK, PERM, TEMP, NOTFOUND;
	}

	private final NetSocket event;

	private final PostfixMapsStoreClient client;

	public PostfixMapsHandler(Vertx vertx, NetSocket event) {
		this.event = event;
		client = PostfixMapsStoreClient.create(vertx);
	}

	@SuppressWarnings("serial")
	private class InvalidQuery extends RuntimeException {
		public InvalidQuery() {
			super("malformed query");
		}
	}

	private record Query(String map, String query) {
	}

	/**
	 * https://www.postfix.org/socketmap_table.5.html
	 */
	@Override
	public void handle(Buffer buffer) {
		Query query = null;
		try {
			query = parseQuery(buffer);
		} catch (InvalidQuery iq) {
			logger.error("Invalid query {}", buffer.toString(), iq);
			sendResponse(ResponseCode.PERM, iq.getMessage());
			return;
		}

		switch (query.map) {
		case "alias" -> client.aliasToMailboxes(query.query).onSuccess(mailboxes -> {
			if (mailboxes.isEmpty()) {
				sendResponse(ResponseCode.NOTFOUND);
				return;
			}

			sendResponse(ResponseCode.OK, mailboxes.stream().collect(Collectors.joining(",")));
		}).onFailure(t -> sendResponse(ResponseCode.TEMP, "alias map fail: " + t.getMessage()));

		case "mailbox" -> client.mailboxExists(query.query).onSuccess(exists -> {
			if (!Boolean.TRUE == exists) {
				sendResponse(ResponseCode.NOTFOUND);
				return;
			}

			sendResponse(ResponseCode.OK, "OK");
		}).onFailure(t -> sendResponse(ResponseCode.TEMP, "mailbox map fail: " + t.getMessage()));

		case "domain" -> client.mailboxDomainsManaged(query.query).onSuccess(managed -> {
			if (!Boolean.TRUE == managed) {
				sendResponse(ResponseCode.NOTFOUND);
				return;
			}

			sendResponse(ResponseCode.OK, "OK");
		}).onFailure(t -> sendResponse(ResponseCode.TEMP, "mailbox domain map fail: " + t.getMessage()));

		case "transport" -> client.getMailboxRelay(query.query).onSuccess(relay -> {
			if (relay == null) {
				sendResponse(ResponseCode.NOTFOUND);
				return;
			}

			sendResponse(ResponseCode.OK, relay);
		}).onFailure(t -> sendResponse(ResponseCode.TEMP, "transport map fail: " + t.getMessage()));

		case "srsrecipient" -> client.srsRecipient(query.query).onSuccess(recipient -> {
			if (recipient == null) {
				sendResponse(ResponseCode.NOTFOUND);
				return;
			}

			sendResponse(ResponseCode.OK, recipient);
		}).onFailure(t -> sendResponse(ResponseCode.TEMP, "srs-recipient map fail: " + t.getMessage()));

		default -> sendResponse(ResponseCode.PERM, "unsupported map name " + query.map);
		}
	}

	/**
	 * Query use https://en.wikipedia.org/wiki/Netstring format<br/>
	 * i.e.:<br/>
	 * <code>12:hello world!,</code>
	 * 
	 * @param buffer
	 * @return
	 */
	private Query parseQuery(Buffer buffer) {
		String cmd = buffer.toString();
		if (Objects.isNull(cmd) || cmd.isEmpty() || !cmd.contains(":") || !cmd.contains(" ")) {
			throw new InvalidQuery();
		}

		int colonIndex = cmd.indexOf(":");
		int spaceIndex = cmd.indexOf(" ");

		int cmdLength = -1;
		try {
			cmdLength = Integer.parseInt(cmd.substring(0, colonIndex));
		} catch (NumberFormatException nfe) {
			throw new InvalidQuery();
		}

		if (cmdLength == -1) {
			throw new InvalidQuery();
		}

		if (cmdLength != cmd.substring(colonIndex + 1).getBytes().length) {
			throw new InvalidQuery();
		}

		return new Query(cmd.substring(colonIndex + 1, spaceIndex).toLowerCase(),
				cmd.substring(spaceIndex + 1).toLowerCase());
	}

	private void sendResponse(ResponseCode code) {
		sendResponse(code, null);
	}

	private void sendResponse(ResponseCode code, String msg) {
		String response = code.name() + " ";

		if (!Strings.isNullOrEmpty(msg)) {
			response += msg;
		}

		event.write(response.length() + ":" + response + ",");
	}
}
