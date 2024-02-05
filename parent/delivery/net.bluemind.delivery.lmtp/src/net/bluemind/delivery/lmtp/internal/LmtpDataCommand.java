/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.delivery.lmtp.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.subethamail.smtp.DropConnectionException;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.io.DotTerminatedInputStream;
import org.subethamail.smtp.io.DotUnstuffingInputStream;
import org.subethamail.smtp.io.ReceivedHeaderStream;
import org.subethamail.smtp.server.BaseCommand;
import org.subethamail.smtp.server.Session;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 */
public class LmtpDataCommand extends BaseCommand {
	private final static int BUFFER_SIZE = 1024 * 32; // 32k seems reasonable

	/** */
	public LmtpDataCommand() {
		super("DATA", "Following text is collected as the message.\n" + "End data with <CR><LF>.<CR><LF>");
	}

	/**
	 * The JDK BufferedInputStream class does something different when subclassed...
	 */
	private static class UnlockedBufferedInputStream extends BufferedInputStream {

		public UnlockedBufferedInputStream(InputStream in, int size) {
			super(in, size);
		}

	}

	/** */
	@Override
	public void execute(String commandString, Session sess) throws IOException, DropConnectionException {
		if (!sess.getHasMailFrom()) {
			sess.sendResponse("503 Error: need MAIL command");
			return;
		} else if (sess.getRecipientCount() == 0) {
			sess.sendResponse("503 Error: need RCPT command");
			return;
		}

		sess.sendResponse("354 End data with <CR><LF>.<CR><LF>");

		InputStream stream = sess.getRawInput();
		stream = new UnlockedBufferedInputStream(stream, BUFFER_SIZE);
		stream = new DotTerminatedInputStream(stream);
		stream = new DotUnstuffingInputStream(stream);
		if (!sess.getServer().getDisableReceivedHeaders()) {
			stream = new ReceivedHeaderStream(stream, sess.getHelo(), sess.getRemoteAddress().getAddress(),
					sess.getServer().getHostName(), sess.getServer().getSoftwareName(), sess.getSessionId(),
					sess.getSingleRecipient());
		}

		try {
			List<RecipientDeliveryStatus> res;
			MessageHandler handler = sess.getMessageHandler();
			if (handler instanceof ILmtpExtendedHandler extHandler) {
				res = extHandler.lmtpData(stream);
			} else {
				sess.getMessageHandler().data(stream);
				res = Collections.singletonList(RecipientAcceptance.ACCEPT.reason("Ok"));
			}

			// Just in case the handler didn't consume all the data, we might as well
			// suck it up so it doesn't pollute further exchanges. This code used to
			// throw an exception, but this seems an arbitrary part of the contract that
			// we might as well relax.
			while (stream.read() != -1)
				;

			for (RecipientDeliveryStatus ra : res) {
				switch (ra.accept()) {
				case PERMANENT_REJECT:
					sess.sendResponse("552 Failed");
					break;
				case TEMPORARY_REJECT:
					sess.sendResponse("452 Temporary failure, reason: " + ra.reason());
					break;
				case ACCEPT:
				default:
					sess.sendResponse("250 Ok");
					break;
				}
			}
		} catch (DropConnectionException ex) {
			throw ex; // Propagate this
		} catch (RejectException ex) {
			sess.sendResponse(ex.getErrorResponse());
		}

		sess.resetMessageState(); // reset session, but don't require new HELO/EHLO
	}
}
