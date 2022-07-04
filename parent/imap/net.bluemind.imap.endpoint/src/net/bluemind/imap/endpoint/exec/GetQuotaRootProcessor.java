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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.GetQuotaRootCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.vertx.Result;
import net.bluemind.mailbox.api.MailboxQuota;

/**
 * 
 * <code>
 * . getquotaroot Inbox
 * * QUOTAROOT Inbox INBOX
 * * QUOTA INBOX (STORAGE 1251 1024000)
 * . OK Completed
 * </code>
 * 
 *
 */
public class GetQuotaRootProcessor extends AuthenticatedCommandProcessor<GetQuotaRootCommand> {

	private static final Logger logger = LoggerFactory.getLogger(GetQuotaRootProcessor.class);

	@Override
	public void checkedOperation(GetQuotaRootCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		SelectedFolder selected = con.select(sc.folder());

		if (selected == null) {
			missingFolder(sc, ctx, completed);
			return;
		}

		MailboxQuota quota = con.quota();

		StringBuilder resp = new StringBuilder();
		resp.append("* QUOTAROOT " + sc.folder() + " INBOX\r\n");
		if (quota != null && quota.quota != null) {
			resp.append("* QUOTA INBOX (STORAGE " + quota.used + " " + quota.quota + ")\r\n");
			logger.debug("Returning quota {}", quota);
		}
		resp.append(sc.raw().tag() + " OK Completed\r\n");

		ctx.write(resp.toString());
		completed.handle(Result.success());
	}

	private void missingFolder(GetQuotaRootCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.write(sc.raw().tag() + " NO Mailbox does not exist\r\n");
		completed.handle(Result.success());
	}

	@Override
	public Class<GetQuotaRootCommand> handledType() {
		return GetQuotaRootCommand.class;
	}

}
