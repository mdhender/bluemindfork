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
package net.bluemind.backend.mail.api.events;

import net.bluemind.backend.mail.api.IMailboxFolders;

/**
 * Helper for knowing important event bus addresses
 *
 */
public class MailEventAddresses {

	private static final String BASE = "mailreplica.";

	/**
	 * 
	 * @param mailboxUniqueId
	 *            uid of the item returned by {@link IMailboxFolders}
	 * @return the event bus address to subscribe
	 */
	public String mailboxContentChanged(String mailboxUniqueId) {
		return BASE + "mailbox.updated." + mailboxUniqueId;
	}

	/**
	 * The received messages on the vertx eventbus address returned by this method
	 * will look like the following:
	 * 
	 * <pre>
	 * <code>
	 * {
	 *   "uid" : "subtree_test1500363938962_lab!user.login^of^user",
	 *   "version" : 5,
	 *   "minor" : false
	 * }
	 * </code>
	 * </pre>
	 * 
	 * Minor changes mean that only the content / annotations of a folder has
	 * changed.
	 * 
	 * Non-minor (aka major) changes represent CRUD actions in the hierarchy of a
	 * mailbox: folder rename, create, update.
	 * 
	 * @param domainUid
	 *            uid of the user's domain
	 * @param userLogin
	 *            localpart of the user login
	 * @return the event bus address to subscribe
	 */
	public static String userMailboxHierarchyChanged(String domainUid, String entryUid) {
		String containerUid = "subtree_" + domainUid.replace('.', '_') + "!user." + entryUid;
		return BASE + "hierarchy.updated." + containerUid;
	}

	/**
	 * The received messages on the vertx eventbus address returned by this method
	 * will look like the following:
	 * 
	 * <pre>
	 * <code>
	 * {
	 *   "uid" : "subtree_test1500363938962_lab!shared^box",
	 *   "version" : 5,
	 *   "minor" : false
	 * }
	 * </code>
	 * </pre>
	 * 
	 * @param domainUid
	 * @param entryUid
	 * @return the event bus address to subscribe
	 */
	public static String sharedMailboxHierarchyChanged(String domainUid, String entryUid) {
		String containerUid = "subtree_" + domainUid.replace('.', '_') + "!" + entryUid;
		return BASE + "hierarchy.updated." + containerUid;
	}

}
