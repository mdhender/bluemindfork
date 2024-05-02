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

import java.util.Arrays;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.dto.Mode;

public class UidStoreCommand extends ReturnOkCommand {

	private final String flags;
	private final Mode mode;
	private final String set;

	public UidStoreCommand(CommandContext ctx, String imapIdSet, Mode m, String... flags) {
		super(ctx);
		this.set = imapIdSet;
		this.mode = m;
		this.flags = Arrays.stream(flags).collect(Collectors.joining(" ", " (", ")"));
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("UID STORE ").appendString(set).appendString(mode.prefix()).appendString("FLAGS.SILENT")
				.appendString(flags);
	}

}
