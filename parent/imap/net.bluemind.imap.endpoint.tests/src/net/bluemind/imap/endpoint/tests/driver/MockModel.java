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
package net.bluemind.imap.endpoint.tests.driver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.endpoint.driver.SelectedFolder;

public class MockModel {

	public static final MockModel INSTANCE = new MockModel();

	Map<String, SelectedFolder> folders;

	private MockModel() {
		this.folders = new ConcurrentHashMap<>();
	}

	public void registerFolder(UUID uid, String fullName) {
		MailboxReplica repl = new MailboxReplica();
		repl.uidValidity = System.currentTimeMillis() / 1000;
		repl.lastUid = 3;
		String[] split = fullName.split("/");
		repl.name = split[split.length - 1];
		repl.fullName = fullName;
		repl.highestModSeq = 42;
		ItemValue<MailboxReplica> item = ItemValue.create(uid.toString(), repl);
		SelectedFolder sf = new SelectedFolder(item, 3, 1);
		folders.put(uid.toString(), sf);

	}

	public SelectedFolder byName(String n) {
		return folders.values().stream().filter(s -> s.folder.value.name.equals(n)).findAny().orElse(null);
	}

}
