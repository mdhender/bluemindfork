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
package net.bluemind.imap.driver.mailapi;

import java.util.Set;

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;

public class NamespacedFolder {

	private final ItemValue<Mailbox> self;
	private final ItemValue<Mailbox> owner;
	private final ItemValue<MailboxReplica> folder;
	private final Set<String> parents;
	private final String mountPoint;

	public NamespacedFolder(ItemValue<Mailbox> self, ItemValue<Mailbox> owner, ItemValue<MailboxReplica> folder,
			Set<String> parents) {
		this.self = self;
		this.owner = owner;
		this.folder = folder;
		this.parents = parents;
		this.mountPoint = fullNameWithMountpoint0();
	}

	public boolean otherMailbox() {
		return owner != self;
	}

	public ItemValue<MailboxReplica> folder() {
		return folder;
	}

	public ItemValue<Mailbox> owner() {
		return owner;
	}

	public Set<String> parents() {
		return parents;
	}

	public boolean virtual() {
		return false;
	}

	/**
	 * Cyrus mountpoints for a shared user box
	 * 
	 * <code>
	 * * LIST (\HasChildren) "/" "Autres utilisateurs/tom"
	 * * LIST (\HasNoChildren) "/" "Autres utilisateurs/tom/Appart"
	 * * LIST (\HasNoChildren) "/" "Autres utilisateurs/tom/Archive"
	 * * LIST (\HasNoChildren) "/" "Autres utilisateurs/tom/Drafts"
	 * * LIST (\HasNoChildren) "/" "Autres utilisateurs/tom/INBOX/autre fils de inbox"
	 * * LIST (\HasNoChildren) "/" "Autres utilisateurs/tom/INBOX/fils de inbox"
	 * </code>
	 * 
	 * @return eg. "Autres utilisateurs/tom"
	 */
	public String fullNameWithMountpoint() {
		return mountPoint;
	}

	private String fullNameWithMountpoint0() {
		if (otherMailbox()) {
			if (owner.value.type.sharedNs) {
				return DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + folder.value.fullName;
			} else {
				if (folder.value.fullName.equals("INBOX")) {
					return DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/" + owner.value.name;
				} else {
					return DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/" + owner.value.name + "/"
							+ folder.value.fullName;
				}
			}
		} else {
			return folder.value.fullName;
		}
	}
}