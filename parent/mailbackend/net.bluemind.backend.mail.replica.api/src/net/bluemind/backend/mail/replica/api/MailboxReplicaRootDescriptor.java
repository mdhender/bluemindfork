/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.Mailbox;

@BMApi(version = "3")
public class MailboxReplicaRootDescriptor {

	@BMApi(version = "3")
	public static enum Namespace {
		users("user.", "user.", false),

		shared("", "", false),

		deletedShared("", "DELETED.", true),

		deleted("user.", "DELETED.user.", true);

		private String prefix;
		private String techPrefix;
		private boolean expunged;

		private Namespace(String pref, String techPref, boolean deleted) {
			this.prefix = pref;
			this.techPrefix = techPref;
			this.expunged = deleted;
		}

		public boolean expunged() {
			return expunged;
		}

		public String prefix() {
			return prefix;
		}

	}

	public Namespace ns;

	/**
	 * john^doe
	 */
	public String name;
	public String dataLocation;

	public String fullName() {
		return ns.prefix() + name;
	}

	public String internalFullName() {
		return ns.techPrefix + name;
	}

	public boolean isRoot(String boxName) {
		return fullName().equals(boxName);
	}

	public static MailboxReplicaRootDescriptor create(Mailbox mbox) {
		MailboxReplicaRootDescriptor rd = new MailboxReplicaRootDescriptor();
		rd.ns = mbox.type.sharedNs ? Namespace.shared : Namespace.users;
		rd.name = mbox.name.replace('.', '^');
		return rd;
	}

	public static MailboxReplicaRootDescriptor create(Namespace ns, String name) {
		MailboxReplicaRootDescriptor rd = new MailboxReplicaRootDescriptor();
		rd.ns = ns;
		rd.name = name.replace('.', '^');
		return rd;
	}

	public static MailboxReplicaRootDescriptor of(String boxName) {
		String name = boxName;
		Namespace ns = Namespace.shared;
		if (boxName.startsWith("user.")) {
			ns = Namespace.users;
			name = name.substring(5);
		}
		int dotIdx = name.indexOf('.');
		if (dotIdx > 0) {
			name = name.substring(0, dotIdx);
		}
		return create(ns, name);
	}

	public String toString() {
		return "RootDesc{ns: " + ns + ", n: " + name + ", fn: " + fullName() + ", loc: " + dataLocation + "}";
	}

	@BMApi(version = "3")
	public static class MailboxReplicaRootUpdate {
		public MailboxReplicaRootDescriptor from;
		public MailboxReplicaRootDescriptor to;
	}

}
