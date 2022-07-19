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

public class UidCopyCommand extends AnalyzedCommand {

	private static final Pattern fetchTemplate = Pattern.compile("uid copy ([^\\s]+) (.*)$", Pattern.CASE_INSENSITIVE);
	private final String folder;
	private final String idset;

	public UidCopyCommand(RawImapCommand raw) {
		super(raw);
		String fetch = flattenAtoms(true).fullCmd;
		Matcher m = fetchTemplate.matcher(fetch);

		if (m.find()) {
			idset = m.group(1);
			folder = m.group(2).replace("\"", "");
		} else {
			throw new EndpointRuntimeException("Cannot analyze copy cmd " + fetch);
		}
	}

	public String folder() {
		return folder;
	}

	public String idset() {
		return idset;
	}

}
