/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.group.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import net.bluemind.core.api.Email;
import net.bluemind.group.api.Group;
import net.bluemind.mailbox.api.Mailbox;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class GroupHelper {
	public static Mailbox groupToMailbox(Group group) {
		Mailbox groupMailbox = new Mailbox();
		groupMailbox.name = "_" + normalizeGroupName(group.name);
		groupMailbox.type = Mailbox.Type.group;

		if (group.mailArchived && group.emails.size() != 0) {
			groupMailbox.routing = Mailbox.Routing.internal;
		} else {
			groupMailbox.routing = Mailbox.Routing.none;
		}

		groupMailbox.emails = new ArrayList<Email>(group.emails.size());
		groupMailbox.emails.addAll(group.emails);
		groupMailbox.archived = false;
		groupMailbox.hidden = true;
		groupMailbox.system = false;
		groupMailbox.dataLocation = group.dataLocation;
		return groupMailbox;
	}

	private static String normalizeGroupName(String groupName) {
		String temp = Normalizer.normalize(groupName, Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

		String normalizedName = pattern.matcher(temp).replaceAll("").toLowerCase();
		pattern = Pattern.compile("[^a-z0-9-._]");
		normalizedName = pattern.matcher(normalizedName).replaceAll("_");

		return normalizedName;
	}
}
