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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.UidSearchCommand;
import net.bluemind.imap.endpoint.locks.ISequenceCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceReader;
import net.bluemind.lib.vertx.Result;

public class UidSearchProcessor extends SelectedStateCommandProcessor<UidSearchCommand>
		implements ISequenceReader, ISequenceCheckpoint {

	private static final Logger logger = LoggerFactory.getLogger(UidSearchProcessor.class);

	@Override
	protected void checkedOperation(UidSearchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		Stopwatch chrono = Stopwatch.createStarted();

		StringBuilder forCheckpoint = new StringBuilder();
		// uid search is allowed to checkpoint but search cannot do that
		checkpointSequences(logger, command.raw().tag() + " uid search", forCheckpoint, ctx);

		List<Long> imapUids = ctx.mailbox().uids(ctx.selected(), command.query());
		if (imapUids == null) {
			ctx.write(command.raw().tag() + " BAD Invalid Search criteria\r\n").onComplete(completed);
			return;
		}
		Map<Long, Integer> visibleUids = ctx.selected().imapUidToSeqNum();
		imapUids = imapUids.stream().filter(visibleUids::containsKey).toList();
		if (imapUids.isEmpty()) {
			long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
			ctx.write(forCheckpoint.toString() + "* SEARCH\r\n" + command.raw().tag() + " OK Completed (took " + ms
					+ "ms)\r\n");
		} else {
			String uidsResp = imapUids.stream().mapToLong(Long::longValue).mapToObj(Long::toString)
					.collect(Collectors.joining(" ", forCheckpoint.toString() + "* SEARCH ",
							"\r\n" + command.raw().tag() + " OK Completed\r\n"));
			ctx.write(uidsResp);
		}
		completed.handle(Result.success());
	}
	
	@Override
	public Class<UidSearchCommand> handledType() {
		return UidSearchCommand.class;
	}

}
