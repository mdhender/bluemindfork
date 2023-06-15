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

import java.util.regex.Pattern;

import net.bluemind.imap.endpoint.driver.ImapIdSet;

public class UidFetchCommand extends AbstractFetchCommand {

	private static final Pattern fetchTemplate = Pattern.compile("uid fetch ([^\\s]+) (.*)$", Pattern.CASE_INSENSITIVE);

	public UidFetchCommand(RawImapCommand raw) {
		super(raw, fetchTemplate);
	}

	@Override
	protected ImapIdSet fromSerializedSet(String set) {
		return ImapIdSet.uids(set);
	}

}
