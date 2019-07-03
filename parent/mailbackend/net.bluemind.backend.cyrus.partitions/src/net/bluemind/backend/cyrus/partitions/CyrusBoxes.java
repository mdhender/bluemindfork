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
package net.bluemind.backend.cyrus.partitions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;

public class CyrusBoxes {

	private static final Logger logger = LoggerFactory.getLogger(CyrusBoxes.class);

	private static final Pattern userMboxRootRe = Pattern.compile("(.*)!user\\.([^\\.]*)$");
	private static final Pattern userMboxRe = Pattern.compile("(.*)!user\\.([^\\.]*)\\.(.*)$");
	private static final Pattern deletedMbox = Pattern.compile("(.*)!DELETED.user\\.([^\\.]*)\\.(.*)$");
	private static final Pattern deletedSharedMbox = Pattern.compile("(.*)!DELETED\\.([^\\.]*)\\.(.*)$");

	public static class ReplicatedBox {
		public Namespace ns;
		public String local;
		public String partition;
		public String folderName;
		public boolean mailboxRoot;

		public String toString() {
			return "[RB " + ns + " local: " + local + ", part: " + partition + ", f: " + folderName + "]";
		}

		public MailboxReplicaRootDescriptor asDescriptor() {
			MailboxReplicaRootDescriptor rd = new MailboxReplicaRootDescriptor();
			rd.ns = ns;
			rd.name = local;
			return rd;
		}
	}

	/**
	 * Computes partition from domain part & mailbox name from local part of a fully
	 * qualified userName.
	 * 
	 * Returns null otherwise.
	 * 
	 * @param userName fully qualified user name
	 * @return a {@link ReplicatedBox}
	 */
	public static ReplicatedBox forLoginAtDomain(String userName) {
		ReplicatedBox box = new ReplicatedBox();
		box.ns = Namespace.users;
		int atIdx = userName.indexOf('@');
		if (atIdx < 0) {
			logger.warn("Username without domain part, can't continue.");
			return null;
		}
		box.local = userName.substring(0, atIdx);
		box.partition = userName.substring(atIdx + 1).replace('.', '_');
		box.folderName = "";
		return box;
	}

	/**
	 * Input is "ex2016.vmw!user.tom.Deleted Messages" (or without the quotes)
	 * 
	 * @param fromBox
	 * @return
	 */
	public static ReplicatedBox forCyrusMailbox(String fromMbox) {
		fromMbox = CharMatcher.is('"').trimFrom(fromMbox);
		Matcher userMatch = userMboxRe.matcher(fromMbox);
		Matcher userRootMatch = userMboxRootRe.matcher(fromMbox);
		Matcher deletedMailboxMatch = deletedMbox.matcher(fromMbox);
		Matcher deletedSharedMailboxMatch = deletedSharedMbox.matcher(fromMbox);

		if (deletedMailboxMatch.find()) {
			String domain = deletedMailboxMatch.group(1);
			String login = deletedMailboxMatch.group(2);
			logger.debug("Extracted p: {}, l: {}", domain, login);
			ReplicatedBox rb = new ReplicatedBox();
			rb.local = login;
			rb.partition = domain.replace('.', '_');
			rb.folderName = deletedMailboxMatch.group(3).replace('.', '/').replace('^', '/');
			rb.ns = Namespace.deleted;
			return rb;
		} else if (deletedSharedMailboxMatch.find()) {
			String domain = deletedSharedMailboxMatch.group(1);
			String login = deletedSharedMailboxMatch.group(2);
			logger.debug("Extracted p: {}, l: {}", domain, login);
			ReplicatedBox rb = new ReplicatedBox();
			rb.local = login;
			rb.partition = domain.replace('.', '_');
			rb.folderName = deletedSharedMailboxMatch.group(3).replace('.', '/').replace('^', '/');
			rb.ns = Namespace.deletedShared;
			return rb;
		} else if (userMatch.find()) {
			String domain = userMatch.group(1);
			String login = userMatch.group(2);
			logger.debug("Extracted p: {}, l: {}", domain, login);
			ReplicatedBox rb = new ReplicatedBox();
			rb.local = login;
			rb.partition = domain.replace('.', '_');
			rb.folderName = userMatch.group(3).replace('.', '/').replace('^', '/');
			rb.ns = Namespace.users;
			return rb;
		} else if (userRootMatch.find()) {
			logger.debug("matched mailbox root for {}", fromMbox);
			String domain = userRootMatch.group(1);
			String login = userRootMatch.group(2);
			logger.debug("Extracted p: {}, l: {}", domain, login);
			ReplicatedBox rb = new ReplicatedBox();
			rb.local = login;
			rb.partition = domain.replace('.', '_');
			rb.folderName = "INBOX";
			rb.ns = Namespace.users;
			rb.mailboxRoot = true;
			return rb;
		} else {
			ReplicatedBox rb = new ReplicatedBox();
			rb.ns = Namespace.shared;
			int mark = fromMbox.indexOf('!');
			rb.partition = fromMbox.substring(0, mark).replace('.', '_');
			String afterPart = fromMbox.substring(mark + 1);
			int dot = afterPart.indexOf('.');
			if (dot > 0) {
				rb.local = afterPart.substring(0, dot);
				rb.folderName = afterPart.substring(dot + 1).replace('.', '/').replace('^', '/');
			} else {
				rb.local = afterPart;
				rb.folderName = afterPart;
				rb.mailboxRoot = true;
			}
			return rb;
		}
	}

}
