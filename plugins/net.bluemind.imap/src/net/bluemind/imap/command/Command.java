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
package net.bluemind.imap.command;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.imap.ITagProducer;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.lib.jutf7.UTF7Converter;

public abstract class Command<T> implements ICommand<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	protected T data;
	private String tag;

	@Override
	public String execute(final IoSession session, ITagProducer tp, Semaphore lock) {
		final CommandArgument args = buildCommand();

		String cmd = args.getCommandString();
		StringBuilder sb = new StringBuilder(10 + cmd.length());
		this.tag = tp.nextTag();
		sb.append(tag);
		sb.append(' ');
		sb.append(cmd);
		String sent = sb.toString();
		logger.debug("C: {}", sent);
		session.setAttribute("activeCommand", sent);
		byte[] literal = args.getLiteralData();
		if (literal != null) {
			WriteFuture future = session.write(sent).addListener(handleClosedConnection(lock));

			if (sent.endsWith("+}")) {
				// cyrus reports IOERROR when pushing everything in one chunk
				future.addListener(iofuture -> {
					logger.debug("op complete: LITERAL+ for {}bytes", args.getLiteralData().length);
					session.write(args.getLiteralData());
				});
			} else {
				lock(lock);
				future = session.write(literal).addListener(handleClosedConnection(lock));
			}
		} else {
			session.write(sent).addListener(handleClosedConnection(lock));
		}

		return tag;
	}

	private IoFutureListener<WriteFuture> handleClosedConnection(Semaphore lock) {
		return new IoFutureListener<WriteFuture>() {

			@Override
			public void operationComplete(WriteFuture future) {
				if (future.getException() instanceof WriteToClosedSessionException) {
					logger.error(future.getException().getMessage());
					lock.release();
				}
			}
		};
	}

	public String taggedResponseReceived(List<IMAPResponse> rs) {
		responseReceived(rs);
		return tag;
	}

	public abstract void responseReceived(List<IMAPResponse> rs);

	private void lock(Semaphore lock) {
		try {
			if (!lock.tryAcquire(5, TimeUnit.SECONDS)) {
				throw new RuntimeException("timeout ");
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public T getReceivedData() {
		return data;
	}

	protected abstract CommandArgument buildCommand();

	protected static String toUtf7(String mailbox) {
		return toUtf7(mailbox, true);
	}

	protected static String toUtf7(String mailbox, boolean quoted) {
		Iterable<String> it = Splitter.on('@').split(mailbox);
		StringBuilder b = new StringBuilder(2 * mailbox.length() + 2);
		if (quoted) {
			b.append("\"");
		}
		String append = "";
		for (String s : it) {
			b.append(append);
			append = "@";
			String ret = UTF7Converter.encode(s);
			b.append(ret);
		}
		if (quoted) {
			b.append("\"");
		}
		return b.toString();
	}

	protected boolean isOk(List<IMAPResponse> rs) {
		return rs.get(rs.size() - 1).isOk();
	}

	protected static String fromUtf7(String mailbox) {
		return UTF7Converter.decode(mailbox);
	}
}
