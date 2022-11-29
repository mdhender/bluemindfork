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

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.base.Splitter;

import net.bluemind.lib.jutf7.UTF7Converter;

public class StatusCommand extends AnalyzedCommand {

	private final List<String> properties;
	private final String folder;

	protected StatusCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(false, 0);
		// just after the '(' in status inbox (unseen)
		int propsIdxStart = -1;
		// before ')'
		int propsIdxEnd = flat.fullCmd.lastIndexOf(')');
		if (flat.literals.length == 1) {
			// folder was given as an atom
			folder = flat.literals[0].toString(StandardCharsets.UTF_8);
			propsIdxStart = flat.fullCmd.indexOf(" (") + 2;
		} else {
			String utf7Folder;
			int folderStart = "status ".length();
			if (flat.fullCmd.charAt(folderStart) == '"') {
				int endQuote = flat.fullCmd.indexOf('"', folderStart + 2);
				utf7Folder = flat.fullCmd.substring(folderStart + 1, endQuote);
				propsIdxStart = endQuote + 3;
			} else {
				int spaceAfterFolder = flat.fullCmd.indexOf(' ', folderStart + 1);
				utf7Folder = flat.fullCmd.substring(folderStart, spaceAfterFolder);
				propsIdxStart = spaceAfterFolder + 2;
			}
			folder = UTF7Converter.decode(utf7Folder);
		}
		properties = Splitter.on(' ').splitToList(flat.fullCmd.substring(propsIdxStart, propsIdxEnd));
	}

	public String folder() {
		return folder;
	}

	public List<String> properties() {
		return properties;
	}

}
