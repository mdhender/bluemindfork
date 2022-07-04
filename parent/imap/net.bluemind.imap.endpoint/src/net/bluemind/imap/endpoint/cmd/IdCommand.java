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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IdCommand extends AnalyzedCommand {

	private static final Pattern quotedString = Pattern.compile("\"([^\"]+)\"");
	private Map<String, String> clientId;

	protected IdCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(true);
		Matcher matcher = quotedString.matcher(flat.fullCmd);
		List<String> kv = new ArrayList<>();
		while (matcher.find()) {
			kv.add(matcher.group(1));
		}
		Map<String, String> asMap = IntStream.range(0, kv.size() / 2).boxed()
				.collect(Collectors.toMap(i -> kv.get(i * 2), i -> kv.get(i * 2 + 1)));
		this.clientId = asMap;
	}

	public Map<String, String> clientId() {
		return clientId;
	}

}
