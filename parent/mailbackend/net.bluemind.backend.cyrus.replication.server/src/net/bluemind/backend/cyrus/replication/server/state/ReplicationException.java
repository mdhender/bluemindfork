/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.server.state;

import java.util.concurrent.CompletionException;

import net.bluemind.backend.cyrus.replication.server.cmd.CommandResult;

@SuppressWarnings("serial")
public class ReplicationException extends RuntimeException {

	public enum ErrorKind {

		serverError(CommandResult.no("IMAP_PROTOCOL_BAD_PARAMETERS")),

		mailboxNonExistent(CommandResult.no("IMAP_MAILBOX_NONEXISTENT No Such Mailbox")),

		malformedMailboxName(CommandResult.no("IMAP_MAILBOX_MALFORMED Malformed mailbox name"));

		private final CommandResult result;

		private ErrorKind(CommandResult res) {
			this.result = res;
		}

		public CommandResult result() {
			return result;
		}

	}

	private final ErrorKind errorKind;

	private ReplicationException(ErrorKind ek, String msg) {
		super(msg);
		this.errorKind = ek;
	}

	private ReplicationException(ErrorKind ek, Throwable cause) {
		super(cause);
		this.errorKind = ek;
	}

	public static final ReplicationException nonExistent(String msg) {
		return new ReplicationException(ErrorKind.mailboxNonExistent, msg);
	}

	public static final ReplicationException malformedMailboxName(String msg) {
		return new ReplicationException(ErrorKind.malformedMailboxName, msg);
	}

	public static final ReplicationException serverError(Throwable t) {
		return new ReplicationException(ErrorKind.serverError, t);
	}

	public static final ReplicationException serverError(String msg) {
		return new ReplicationException(ErrorKind.serverError, msg);
	}

	public ErrorKind errorKind() {
		return errorKind;
	}

	public CommandResult asResult() {
		return errorKind.result;
	}

	public static ReplicationException cast(Throwable t) {
		if (t instanceof CompletionException) {
			return cast(t.getCause());
		} else if (t instanceof ReplicationException) {
			return ReplicationException.class.cast(t);
		} else {
			return null;
		}
	}

}
