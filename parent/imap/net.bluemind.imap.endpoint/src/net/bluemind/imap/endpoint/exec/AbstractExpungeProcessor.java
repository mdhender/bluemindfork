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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Verify;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.driver.ImapIdSet;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.SelectedMessage;
import net.bluemind.imap.endpoint.driver.UpdateMode;
import net.bluemind.imap.endpoint.locks.ISequenceCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceWriter;

public abstract class AbstractExpungeProcessor<T extends AnalyzedCommand> extends SelectedStateCommandProcessor<T>
		implements ISequenceWriter, ISequenceCheckpoint {

	private static final Logger logger = LoggerFactory.getLogger(AbstractExpungeProcessor.class);
	private final boolean onlyCheckpointed;

	public AbstractExpungeProcessor(boolean onlyCheckpointed) {
		this.onlyCheckpointed = onlyCheckpointed;
	}

	@Override
	protected void checkedOperation(T command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection mailbox = ctx.mailbox();
		SelectedFolder folder = ctx.selected();
		SelectedMessage[] uidToSequence = folder.sequences;
		List<Long> uids = mailbox.uidSet(folder, fromSet(command), ItemFlagFilter.create().must(ItemFlag.Deleted),
				onlyCheckpointed);
		logger.info("{} imap visible message(s), {} to expunge", uidToSequence.length, uids.size());
		Verify.verifyNotNull(uids);

		if (!uids.isEmpty()) {
			ImapIdSet asSet = ImapIdSet.uids(uids.stream().map(Object::toString).collect(Collectors.joining(",")));
			long version = mailbox.updateFlags(folder, asSet, UpdateMode.Add, Collections.singletonList("\\Expunged"));
			ctx.nexus().dispatchSequencesChanged(mailbox, command.raw().tag(), folder.folder.uid, version);
		}

		StringBuilder resps = new StringBuilder();
		checkpointSequences(logger, command.raw().tag() + " expunge", resps, ctx);
		ctx.write(resps.toString() + command.raw().tag() + " OK Completed\r\n").onComplete(completed);
	}

	protected abstract ImapIdSet fromSet(T command);

}
