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
package net.bluemind.imap.endpoint.cmd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.lib.jutf7.UTF7Converter;

public class RenameCommand extends AnalyzedCommand {
	private static final Pattern quotedString = Pattern.compile("rename \"?([^\"]+)\"? \"?([^\"]+)",
			Pattern.CASE_INSENSITIVE);

	private String srcFolder;
	private String dstFolder;

	protected RenameCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(true, 0);
		Matcher matcher = quotedString.matcher(flat.fullCmd);
		if (matcher.find()) {
			this.srcFolder = UTF7Converter.decode(matcher.group(1));
			this.dstFolder = UTF7Converter.decode(matcher.group(2));
		} else {
			throw new EndpointRuntimeException("Failed to extract folder names out of '" + flat.fullCmd + "'");
		}
	}

	public String srcFolder() {
		return srcFolder;
	}

	public String dstFolder() {
		return dstFolder;
	}
}
