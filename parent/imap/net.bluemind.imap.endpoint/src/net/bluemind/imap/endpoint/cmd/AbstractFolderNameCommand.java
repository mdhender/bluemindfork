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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public abstract class AbstractFolderNameCommand extends AnalyzedCommand {

	private final String folder;

	protected AbstractFolderNameCommand(RawImapCommand raw, Pattern extractionRe) {
		super(raw);
		FlatCommand flat = flattenAtoms(true);
		Matcher matcher = extractionRe.matcher(flat.fullCmd);
		if (matcher.find()) {
			String tmpFolder = matcher.group(1);
			if ("inbox".equalsIgnoreCase(tmpFolder)) {
				tmpFolder = "INBOX";
			}
			this.folder = tmpFolder;
			folderExtracted(matcher);
		} else {
			throw new EndpointRuntimeException("Failed to extract folder name out of '" + flat.fullCmd + "'");
		}
	}

	/**
	 * This is called with matcher.find() returning true and folder being group 1.
	 * 
	 * @param matcher
	 */
	protected void folderExtracted(@SuppressWarnings("unused") Matcher matcher) {

	}

	public final String folder() {
		return folder;
	}

}
