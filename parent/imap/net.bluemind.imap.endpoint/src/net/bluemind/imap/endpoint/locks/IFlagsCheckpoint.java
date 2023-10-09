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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.imap.endpoint.locks;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.driver.SelectedFolder;

public interface IFlagsCheckpoint {

	default Set<String> checkpointFlags(Logger logger, String checkpointCause, ImapContext ctx, StringBuilder resp) {
		SelectedFolder liveBox = ctx.mailbox().refreshed(ctx.selected());
		return checkpointFlags(logger, checkpointCause, ctx, resp, liveBox);
	}

	default Set<String> checkpointFlags(Logger logger, String checkpointCause, ImapContext ctx, StringBuilder resp,
			SelectedFolder liveBox) {
		SetView<String> permaFlagsChanges = Sets.difference(Set.copyOf(liveBox.labels),
				Set.copyOf(ctx.selected().labels));
		if (!permaFlagsChanges.isEmpty()) {
			String extra = extraLabels(liveBox);
			resp.append("* FLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen" + extra + ")\r\n");
			resp.append("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen \\*)] Ok\r\n");
			logger.debug("[{}] Flags checkpoint triggered by {} found changes", ctx.logConnectionId(), checkpointCause);
		}
		return Set.copyOf(liveBox.labels);
	}

	private String extraLabels(SelectedFolder selected) {
		String labels = selected.labels.stream().collect(Collectors.joining(" "));
		if (!labels.isBlank()) {
			return " " + labels;
		} else {
			return "";
		}
	}

}
