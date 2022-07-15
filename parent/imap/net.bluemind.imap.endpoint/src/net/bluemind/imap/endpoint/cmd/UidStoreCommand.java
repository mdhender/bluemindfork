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

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;

import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.driver.UpdateMode;

/**
 * 
 * <code>uid store 19 +flags (\seen \deleted)</code>
 * 
 *
 */
public class UidStoreCommand extends AnalyzedCommand {

	private static final Pattern fetchTemplate = Pattern.compile("uid store ([^\\s]+) (.*)$", Pattern.CASE_INSENSITIVE);
	private String idset;
	private List<String> flags;
	private UpdateMode mode;
	private boolean silent;

	public UidStoreCommand(RawImapCommand raw) {
		super(raw);
		String fetch = flattenAtoms(true).fullCmd;
		Matcher m = fetchTemplate.matcher(fetch);

		if (m.find()) {
			idset = m.group(1);
			parseFlags(m);
		} else {
			throw new EndpointRuntimeException("Cannot analyze store cmd " + fetch);
		}
	}

	private void parseFlags(Matcher m) {
		String flagOrig = m.group(2);
		String flagChange = flagOrig.toLowerCase();
		char start = flagChange.charAt(0);
		switch (start) {
		case '+':
			mode = UpdateMode.Add;
			break;
		case '-':
			mode = UpdateMode.Remove;
			break;
		default:
		case 'f':
			mode = UpdateMode.Replace;
			break;
		}
		silent = flagChange.contains(".silent");
		int startIdx = flagChange.indexOf('(');
		int endIdx = flagChange.indexOf(')');
		flags = Splitter.on(' ').omitEmptyStrings().splitToList(flagOrig.substring(startIdx + 1, endIdx));
	}

	public String idset() {
		return idset;
	}

	public UpdateMode mode() {
		return mode;
	}

	public boolean silent() {
		return silent;
	}

	public List<String> flags() {
		return flags;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(UidStoreCommand.class).add("set", idset).add("m", mode).add("s", silent)
				.add("f", flags).toString();
	}

}
