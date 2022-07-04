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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public abstract class AbstractListCommand extends AnalyzedCommand {

	private static final Pattern quotedString = Pattern.compile("\"([^\"]*)\"");
	private String reference;
	private String mailboxPattern;

	/**
	 * xlist "" "%"
	 * 
	 * @param raw
	 */
	protected AbstractListCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(true);
		List<String> refAndBoxNamePattern = new ArrayList<>();
		Matcher match = quotedString.matcher(flat.fullCmd);
		while (match.find()) {
			refAndBoxNamePattern.add(match.group(1));
		}
		if (refAndBoxNamePattern.size() >= 2) {
			this.reference = refAndBoxNamePattern.get(0);
			this.mailboxPattern = refAndBoxNamePattern.get(1);
		} else {
			throw new EndpointRuntimeException("reference & mailbox pattern missing from '" + flat.fullCmd + "'");
		}
	}

	public String reference() {
		return reference;
	}

	public String mailboxPattern() {
		return mailboxPattern;
	}

}
