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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.endpoint.driver.ImapMailbox;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.SelectedMessage;
import net.bluemind.mailbox.api.Mailbox;

public class MockModel {

	public static final MockModel INSTANCE = new MockModel();

	Map<String, SelectedFolder> folders;

	private MockModel() {
		this.folders = new ConcurrentHashMap<>();
	}

	public String toString() {
		return folders.values().stream().map(f -> f.folder.value.fullName).collect(Collectors.joining(","));
	}

	public void registerFolder(UUID uid, String fullName) {
		Mailbox mb = new Mailbox();
		mb.name = "mock";
		ItemValue<Mailbox> mbItem = ItemValue.create("mock-mailbox-uid", mb);

		MailboxReplica repl = new MailboxReplica();
		repl.uidValidity = System.currentTimeMillis() / 1000;
		repl.lastUid = 3;
		String[] split = fullName.split("/");
		repl.name = split[split.length - 1];
		repl.fullName = fullName;
		repl.highestModSeq = 42;
		ItemValue<MailboxReplica> item = ItemValue.create(uid.toString(), repl);
		item.displayName = repl.name;

		ImapMailbox imapBox = new ImapMailbox();
		imapBox.owner = mbItem;

		SelectedFolder sf = new SelectedFolder(imapBox, item, null, "part_bidon", 3, 1, List.of("NotJunk"), 42L,
				new SelectedMessage[0]);
		folders.put(uid.toString(), sf);
	}

	public SelectedFolder byName(String n) {
		return folders.values().stream().filter(s -> s.folder.value.fullName.equals(n)).findAny().orElse(null);
	}

	public boolean remove(SelectedFolder selectedFolder) {
		return (this.folders.remove(selectedFolder.folder.uid) != null);
	}

	public String rename(SelectedFolder selectedFolder, String newName) {
		selectedFolder.folder.value.fullName = newName;
		selectedFolder.folder.value.name = newName.substring(newName.lastIndexOf('/') + 1);
		return newName;
	}

	public MockModel reset() {
		this.folders = new ConcurrentHashMap<>();
		return this;
	}

}
