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
package net.bluemind.imap.endpoint.tests;

import java.util.Collections;

import io.netty.buffer.Unpooled;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.RawCommandAnalyzer;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.parsing.Part;

public class RawCommand {

	private RawCommand() {
	}

	public static AnalyzedCommand analyzed(String s) {
		Part part = Part.endOfCommand(Unpooled.wrappedBuffer(s.getBytes()));
		RawImapCommand raw = new RawImapCommand(Collections.singletonList(part));
		return new RawCommandAnalyzer().analyze(raw);
	}

}
