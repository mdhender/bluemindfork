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
package net.bluemind.imap.endpoint.exec;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.SearchCommand;
import net.bluemind.lib.vertx.Result;

public class SearchProcessor extends SelectedStateCommandProcessor<SearchCommand> {

	@Override
	public Class<SearchCommand> handledType() {
		return SearchCommand.class;
	}

	@Override
	protected void checkedOperation(SearchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		Stopwatch chrono = Stopwatch.createStarted();

		List<Long> imapUids = ctx.mailbox().uids(ctx.selected(), command.query());

		if (imapUids == null) {
			ctx.write("BAD Invalid Search criteria\r\n");
			completed.handle(Result.success());
			return;
		}
		Map<Long, Integer> imapToSeq = ctx.selected().imapUidToSeqNum();
		List<Integer> asSequences = imapUids.stream().map(imapToSeq::get).filter(Objects::nonNull).toList();
		if (asSequences.isEmpty()) {
			long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
			ctx.write("* SEARCH\r\n" + command.raw().tag() + " OK Completed (took " + ms + "ms)\r\n");
		} else {
			String uidsResp = asSequences.stream().mapToInt(Integer::intValue).mapToObj(Integer::toString)
					.collect(Collectors.joining(" ", "* SEARCH ", "\r\n" + command.raw().tag() + " OK Completed\r\n"));
			ctx.write(uidsResp);
		}
		completed.handle(Result.success());

	}

}
