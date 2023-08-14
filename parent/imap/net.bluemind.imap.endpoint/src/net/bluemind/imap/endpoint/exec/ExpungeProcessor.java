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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.exec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Verify;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.ExpungeCommand;
import net.bluemind.imap.endpoint.driver.ImapIdSet;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.UpdateMode;
import net.bluemind.lib.vertx.Result;

public class ExpungeProcessor extends SelectedStateCommandProcessor<ExpungeCommand> {

	private static final Logger logger = LoggerFactory.getLogger(ExpungeProcessor.class);

	@Override
	public Class<ExpungeCommand> handledType() {
		return ExpungeCommand.class;
	}

	@Override
	protected void checkedOperation(ExpungeCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection mailbox = ctx.mailbox();
		SelectedFolder folder = ctx.selected();
		Map<Long, Integer> uidToSequence = mailbox.sequences(folder);
		List<Long> uids = mailbox.uidSet(folder, "1:*", ItemFlagFilter.create().must(ItemFlag.Deleted));

		Verify.verifyNotNull(uids);

		StringBuilder resps = new StringBuilder();
		if (!uids.isEmpty()) {
			System.err.println("TO_EXPUNGE " + uids.size());
			ImapIdSet asSet = ImapIdSet.uids(uids.stream().map(Object::toString).collect(Collectors.joining(",")));
			mailbox.updateFlags(folder, asSet, UpdateMode.Add, Collections.singletonList("\\Expunged"));
			// imaptest does not yell if we don't tell it what was expunge
			uids.stream().map(uidToSequence::get).filter(Objects::nonNull).sorted(Collections.reverseOrder())
					.forEachOrdered(seq -> resps.append("* " + seq + " EXPUNGE\r\n"));
		}
		ctx.write(resps.toString() + command.raw().tag() + " OK Completed\r\n");
		completed.handle(Result.success());
		logger.debug("{} expunged.", folder);
	}

}
