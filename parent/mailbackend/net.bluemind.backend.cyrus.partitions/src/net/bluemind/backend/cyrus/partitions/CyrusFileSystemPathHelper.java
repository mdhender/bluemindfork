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
	public static final String META_ROOT = "/var/spool/cyrus/meta/";
	public static final String ARCHIVE_ROOT = "/var/spool/bm-hsm/cyrus-archives/";

	private CyrusFileSystemPathHelper() {

	}

	/**
	 * Map letter to cyrus folder name. Used for domain or mailboxname
	 * 
	 * @param uid
	 *                domain or mailboxname first letter
	 * @return cyrus folder name
	 */
	public static char mapLetter(char letter) {
		if (!Character.isLetter(letter)) {
			return 'q';
		}

		return Character.toLowerCase(letter);
	}

	private static String getDomainFileSystemPath(String root, CyrusPartition partition, String domainUid) {
		StringBuilder path = new StringBuilder();
		path.append(root);
		path.append(partition.name);
		path.append("/domain/");
		path.append(mapLetter(domainUid.charAt(0)));
		path.append("/");
		path.append(domainUid);

		return path.toString();
	}

	private static String getPath(String domainUid, MailboxDescriptor mboxDescriptor, CyrusPartition partition,
			long imapUid, String root) {

		char mboxLetter = mboxDescriptor.mailboxName.charAt(0);
		if (mboxDescriptor.type.sharedNs) {
			return sharedPath(domainUid, mboxDescriptor, partition, imapUid, root, mapLetter(domainUid.charAt(0)),
					mboxLetter);
		} else {
			return userPath(domainUid, mboxDescriptor, partition, imapUid, root, mapLetter(domainUid.charAt(0)),
					mboxLetter);
		}
	}

	private static String userPath(String domainUid, MailboxDescriptor mboxDescriptor, CyrusPartition partition,
			long imapUid, String root, char domainLetter, char mboxLetter) {
		mboxLetter = mapLetter(mboxLetter);

		String boxName = mboxDescriptor.mailboxName.replace('.', '^');

		StringBuilder path = new StringBuilder();
		path.append(getDomainFileSystemPath(root, partition, domainUid));
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
		mboxLetter = mapLetter(mboxLetter);

		StringBuilder path = new StringBuilder();
		path.append(getDomainFileSystemPath(root, partition, domainUid));
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

	public static String getDomainDataFileSystemPath(CyrusPartition partition, String domainUid) {
		return getDomainFileSystemPath(MAIN_ROOT, partition, domainUid);
	}

	public static String getDomainMetaFileSystemPath(CyrusPartition partition, String domainUid) {
		return getDomainFileSystemPath(META_ROOT, partition, domainUid);
	}

	public static String getDomainHSMFileSystemPath(CyrusPartition partition, String domainUid) {
		return getDomainFileSystemPath(ARCHIVE_ROOT, partition, domainUid);
	}
}
