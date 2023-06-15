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
package net.bluemind.imap.endpoint.cmd;

import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.cmd.CommandReader.ImapReaderException;
import net.bluemind.lib.jutf7.UTF7Converter;

public abstract class AbstractFolderNameCommand extends AnalyzedCommand {

	private final String folder;

	protected AbstractFolderNameCommand(RawImapCommand raw, String cmd) {
		super(raw);
		FlatCommand flat = flattenAtoms(false);
		CommandReader reader = new CommandReader(flat);
		try {
			reader.command(cmd);
			String tmpFolder = reader.nextString();
			if ("inbox".equalsIgnoreCase(tmpFolder)) {
				tmpFolder = "INBOX";
			}
			this.folder = UTF7Converter.decode(tmpFolder);
		} catch (ImapReaderException ire) {
			throw new EndpointRuntimeException("Failed to extract folder name out of '" + flat.fullCmd + "'", ire);
		}
	}

	public final String folder() {
		return folder;
	}

}
