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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public class UidExpungeCommand extends AnalyzedCommand {

	private static final Logger logger = LoggerFactory.getLogger(UidExpungeCommand.class);
	private static final Pattern quotedString = Pattern.compile("uid expunge (.*)$", Pattern.CASE_INSENSITIVE);
	private String idset;

	/**
	 * uid search all undeleted
	 * 
	 * @param raw
	 */
	protected UidExpungeCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(true);
		Matcher match = quotedString.matcher(flat.fullCmd);
		if (match.find()) {
			this.idset = match.group(1);
			logger.info("q: {}", idset);
		} else {
			throw new EndpointRuntimeException("unknown uid expunge format '" + flat.fullCmd + "'");
		}
	}

	public String idset() {
		return idset;
	}

}
