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
import net.bluemind.imap.endpoint.cmd.GetAclCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.vertx.Result;

/**
 * 
 * ACL capability required.
 * 
 * https://www.rfc-editor.org/rfc/rfc4314.html
 * 
 * <code>
 * . getacl inbox
 * * ACL inbox admin0 lrswipkxtea cli-created-1d27aa9f-717c-3f6d-8316-6849ee36894f@f8de2c4a.internal lrswipkxtea
 * . OK Completed
 * </code>
 * 
 *
 */
public class GetAclProcessor extends AuthenticatedCommandProcessor<GetAclCommand> {

	private static final Logger logger = LoggerFactory.getLogger(GetAclProcessor.class);

	@Override
	public void checkedOperation(GetAclCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		SelectedFolder selected = con.select(sc.folder());

		if (selected == null) {
			missingFolder(sc, ctx, completed);
			return;
		}

		StringBuilder resp = new StringBuilder();
		resp.append("* ACL \"" + sc.folder() + "\" me lrswipkxtea\r\n");
		resp.append(sc.raw().tag() + " OK Completed\r\n");
		if (logger.isWarnEnabled()) {
			logger.warn("Full rights hardcoded for {}", sc.folder());
		}

		ctx.write(resp.toString());
		completed.handle(Result.success());
	}

	private void missingFolder(GetAclCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.write(sc.raw().tag() + " NO Mailbox does not exist\r\n");
		completed.handle(Result.success());
	}

	@Override
	public Class<GetAclCommand> handledType() {
		return GetAclCommand.class;
	}

}
