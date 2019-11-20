/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

public class CyrusFileSystemPathHelper {
	public static final String MAIN_ROOT = "/var/spool/cyrus/data/";
	public static final String ARCHIVE_ROOT = "/var/spool/bm-hsm/cyrus-archives/";

	private CyrusFileSystemPathHelper() {

	}

	private static String getPath(String domainUid, MailboxDescriptor mboxDescriptor, CyrusPartition partition,
			long imapUid, String root) {

		char domainLetter = domainUid.charAt(0);
		if (!Character.isLetter(domainLetter)) {
			domainLetter = 'q';
		}

		char mboxLetter = mboxDescriptor.mailboxName.charAt(0);
		if (mboxDescriptor.type.sharedNs) {
			return sharedPath(domainUid, mboxDescriptor, partition, imapUid, root, domainLetter, mboxLetter);
		} else {
			return userPath(domainUid, mboxDescriptor, partition, imapUid, root, domainLetter, mboxLetter);
		}
	}

	private static String userPath(String domainUid, MailboxDescriptor mboxDescriptor, CyrusPartition partition,
			long imapUid, String root, char domainLetter, char mboxLetter) {
		if (!Character.isLetter(mboxLetter)) {
			mboxLetter = 'q';
		}

		String boxName = mboxDescriptor.mailboxName.replace('.', '^');

		StringBuilder path = new StringBuilder();
		path.append(root);
		path.append(partition.name);
		path.append("/domain/");
		path.append(domainLetter);
		path.append("/");
		path.append(domainUid);
		path.append("/");
		path.append(mboxLetter);
		path.append("/user/");
		path.append(boxName);
		if (!"INBOX".equals(mboxDescriptor.utf7FolderPath)) {
			path.append("/");
			path.append(mboxDescriptor.utf7FolderPath.replace('.', '^'));
		}
		path.append("/");
		path.append(imapUid);
		path.append(".");

		return path.toString();
	}

	private static String sharedPath(String domainUid, MailboxDescriptor mboxDescriptor, CyrusPartition partition,
			long imapUid, String root, char domainLetter, char mboxLetter) {
		if (!mboxDescriptor.mailboxName.equals(mboxDescriptor.utf7FolderPath)) {
			mboxLetter = mboxDescriptor.utf7FolderPath.substring(mboxDescriptor.mailboxName.length() + 1).toLowerCase()
					.charAt(0);
		}
		if (!Character.isLetter(mboxLetter)) {
			mboxLetter = 'q';
		}

		StringBuilder path = new StringBuilder();
		path.append(root);
		path.append(partition.name);
		path.append("/domain/");
		path.append(domainLetter);
		path.append("/");
		path.append(domainUid);
		path.append("/");
		path.append(mboxLetter);
		path.append("/");
		path.append(mboxDescriptor.utf7FolderPath.replace('.', '^'));
		path.append("/");
		path.append(imapUid);
		path.append(".");

		return path.toString();
	}

	public static String getFileSystemPath(String domainUid, MailboxDescriptor mboxDescriptor, CyrusPartition partition,
			long imapUid) {
		return getPath(domainUid, mboxDescriptor, partition, imapUid, MAIN_ROOT);
	}

	public static String getHSMFileSystemPath(String domainUid, MailboxDescriptor mboxDescriptor,
			CyrusPartition partition, long imapUid) {
		return getPath(domainUid, mboxDescriptor, partition, imapUid, ARCHIVE_ROOT);
	}

}
