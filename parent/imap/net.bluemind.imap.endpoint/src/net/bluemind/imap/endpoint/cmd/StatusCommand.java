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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

public class StatusCommand extends AbstractFolderNameCommand {

	private static final Pattern quotedString = Pattern.compile("status \"??([^\"\\s]+)\"?? \\(([^\\)]+)\\)$",
			Pattern.CASE_INSENSITIVE);
	private List<String> properties;

	protected StatusCommand(RawImapCommand raw) {
		super(raw, quotedString);
	}

	@Override
	protected void folderExtracted(Matcher matcher) {
		String props = matcher.group(2);
		this.properties = Splitter.on(' ').omitEmptyStrings().splitToList(props);
	}

	public List<String> properties() {
		return properties;
	}

}
