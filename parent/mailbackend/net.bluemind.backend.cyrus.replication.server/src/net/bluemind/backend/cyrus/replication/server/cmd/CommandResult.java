/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.server.cmd;

import java.util.Objects;
import java.util.Optional;

import org.vertx.java.core.buffer.Buffer;

public class CommandResult {

	public static enum Status {
		OK, NO, BAD;
	}

	private final Status status;
	private final String message;
	private final Optional<Buffer> fullResponse;

	protected CommandResult(Status st, String msg, Buffer buf) {
		this.status = st;
		this.message = msg;
		this.fullResponse = Optional.ofNullable(buf);
	}

	protected CommandResult(Status st, String msg) {
		this(st, msg, null);
	}

	public static CommandResult fromBuffer(Buffer buf) {
		Objects.requireNonNull(buf);
		return new CommandResult(Status.OK, String.format("Buffer with %d byte(s)", buf.length()), buf);
	}

	public static CommandResult success() {
		return success("success");
	}

	public static CommandResult success(String string) {
		return new CommandResult(Status.OK, string);
	}

	public static CommandResult no(String msg) {
		return new CommandResult(Status.NO, msg);
	}

	public static CommandResult bad(String msg) {
		return new CommandResult(Status.BAD, msg);
	}

	public static CommandResult error(Throwable msg) {
		return new CommandResult(Status.NO, msg.getMessage());
	}

	public String toString() {
		return fullResponse//
				.map(b -> String.format("[Buffer with %d byte(s)]", b.length()))
				.orElseGet(() -> String.format("[%s: '%s']", getClass().getSimpleName(), responseString()));
	}

	public boolean hasFullResponse() {
		return fullResponse.isPresent();
	}

	public String responseString() {
		return String.format("%s %s", status.name(), message);
	}

	public Buffer responseBuffer() {
		return fullResponse.map(b -> b).orElse(new Buffer(responseString()));
	}

}
