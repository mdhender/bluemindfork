/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.exec;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.AbstractListCommand;
import net.bluemind.imap.endpoint.driver.ListNode;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.lib.vertx.Result;

/**
 * 
 * <code>
 * . xlist "" "%"
 * * XLIST (\HasNoChildren \Inbox) "/" INBOX
 * * XLIST (\HasChildren) "/" 000_Luxembourg
 * * XLIST (\HasChildren) "/" 001_Bahamas
 * * XLIST (\HasChildren) "/" 002_Turkey
 * * XLIST (\HasChildren) "/" 003_Australia
 * * XLIST (\HasChildren) "/" 004_Tuvalu
 * * XLIST (\HasChildren) "/" 005_Germany
 * * XLIST (\HasNoChildren \drafts) "/" Drafts
 * * XLIST (\HasNoChildren \junk) "/" Junk
 * * XLIST (\HasNoChildren) "/" Outbox
 * * XLIST (\HasNoChildren \sent) "/" Sent
 * * XLIST (\HasNoChildren) "/" Templates
 * * XLIST (\HasNoChildren \trash) "/" Trash
 * . OK Completed (0.027 secs 49 calls)
 * </code>
 *
 * @param <T>
 */
public abstract class AbstractListProcessor<T extends AbstractListCommand> extends AuthenticatedCommandProcessor<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractListProcessor.class);

	private final String listCommand;

	private final boolean withSpecialUse;

	protected AbstractListProcessor(String listCommand, boolean withSpecialUse) {
		this.listCommand = listCommand;
		this.withSpecialUse = withSpecialUse;
	}

	@Override
	public void checkedOperation(T sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		long time = System.currentTimeMillis();
		List<ListNode> folders = con.list(sc.reference(), sc.mailboxPattern());

		StringBuilder sb = new StringBuilder();
		render(folders, sb);
		sb.append(sc.raw().tag() + " OK Completed\r\n");

		ctx.write(sb.toString());
		time = System.currentTimeMillis() - time;
		logger.info("{} completed in {}ms.", listCommand, time);

		completed.handle(Result.success());
	}

	private void render(List<ListNode> folders, StringBuilder sb) {
		for (ListNode ln : folders) {
			sb.append("* ").append(listCommand).append(" (");
			sb.append(ln.hasChildren ? "\\HasChildren" : "\\HasNoChildren");
			if (withSpecialUse && !ln.specialUse.isEmpty()) {
				sb.append(ln.specialUse.stream().collect(Collectors.joining(" ", " ", "")));
			}
			sb.append(") \"/\" ");

			sb.append("\"").append(UTF7Converter.encode(ln.replica.value.fullName)).append("\"\r\n");
		}
	}

}
