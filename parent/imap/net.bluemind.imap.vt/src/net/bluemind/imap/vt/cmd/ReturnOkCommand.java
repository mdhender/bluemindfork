/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.vt.cmd;

import java.util.List;

import org.slf4j.Logger;

import net.bluemind.imap.vt.parsing.IncomingChunk;

public abstract class ReturnOkCommand extends TaggedCommand<Boolean> {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ReturnOkCommand.class);

	protected ReturnOkCommand(CommandContext ctx) {
		super(ctx);
	}

	@Override
	protected Boolean processChunks(List<IncomingChunk> chunks) {
		boolean ok = chunks.getLast().isOk();
		if (!ok) {
			logger.warn("Command did not succeed {}", chunks.getLast());
		}
		return ok;
	}

}
