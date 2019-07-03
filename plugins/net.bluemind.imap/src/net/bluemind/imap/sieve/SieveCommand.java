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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.sieve;

import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SieveCommand<T> {

	protected T retVal;
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private static final byte[] CRLF = "\r\n".getBytes();

	public void execute(IoSession session) {
		if (session == null) {
			logger.error("can't execute command because session was closed");
			throw new RuntimeException("can't execute command because session was closed");
		}

		List<SieveArg> cmd = buildCommand();
		StringBuilder v = new StringBuilder();
		for (int i = 0; i < cmd.size(); i++) {

			SieveArg arg = cmd.get(i);
			if (arg.isLiteral()) {
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				sb.append(arg.getRaw().length);
				sb.append("+}");

				v.append(sb.toString());
				v.append("\n");
				v.append(new String(arg.getRaw()));
				session.write(sb.toString().getBytes());
				session.write(CRLF);
				session.write(arg.getRaw());
			} else {

				session.write(arg.getRaw());
				v.append(new String(arg.getRaw()));
				if (i < cmd.size() - 1) {
					session.write(new byte[] { (byte) ' ' });
					v.append(" ");
				}

			}

		}
		session.write(CRLF);
	}

	public abstract void responseReceived(SieveResponse sr);

	protected abstract List<SieveArg> buildCommand();

	protected boolean commandSucceeded(SieveResponse rs) {
		if (rs == null) {
			logger.debug("session was closed during command");
			return false;
		} else {
			return rs.getMessageResponse().startsWith("OK");
		}
	}

	protected void reportErrors(SieveResponse sr) {
		logger.error(sr.getMessageResponse());
	}

	public T getReceivedData() {
		return retVal;
	}

}
