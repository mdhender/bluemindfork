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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns specific replica container UIDs.
 */
@BMApi(version = "3")
@Path("/mailreplica/uids")
public interface IMailReplicaUids {

	public static final String MAILBOX_RECORDS = "mailbox_records";
	public static final String MAILBOX_RECORDS_PREFIX = "mbox_records_";
	public static final String REPLICATED_MBOXES = "replicated_mailboxes";
	public static final String REPLICATED_CONVERSATIONS = "replicated_conversations";

	/**
	 * Repair operation id for repairing the subtrees containers
	 */
	public static final String REPAIR_SUBTREE_OP = "replication.subtree";

	public static final String REPAIR_RENAMED_INBOX_OP = "renamed.inbox";

	/**
	 * Repair operation id for repairing the multiple inbox syndrom
	 */
	public static final String REPAIR_MINBOX_OP = "replication.minbox";

	public static final String REPAIR_RECS_IN_DIR = "replication.records.in.dir";

	public static final String REPAIR_MESSAGE_BODIES = "message_bodies";

	/**
	 * Given the uid of a {@link MailboxFolder} item, computes the uid for
	 * {@link MailboxItem} container.
	 * 
	 * @param mailboxUniqueId
	 * @return records container uid
	 */
	@GET
	@Path("{uid}/_mailbox")
	public default String getMboxRecords(@PathParam("uid") String mailboxUniqueId) {
		return mboxRecords(mailboxUniqueId);
	}

	/**
	 * Given a records container uid, computes the uid of the the
	 * {@link MailboxFolder} item in the subtree container.
	 * 
	 * @param recordsContainerUid
	 * @return {@link MailboxFolder} uid for the given records container
	 */
	@GET
	@Path("{uid}/_mailbox_record")
	public static String getUniqueId(@PathParam("uid") String recordsContainerUid) {
		return uniqueId(recordsContainerUid);
	}

	/**
	 * Returns the uid for the subtree container for a given mailbox
	 * 
	 * @param domainUid
	 * @param mbox      Mailbox item
	 * @return subtree container UID
	 */
	@GET
	@Path("{domain}/_mailbox_subtree")
	public default String getSubtreeUid(@PathParam("domain") String domainUid, ItemValue<Mailbox> mbox) {
		return subtreeUid(domainUid, mbox);
	}

	/**
	 * Returns the unique identifier of the conversation subtree container for the
	 * given mailbox.
	 * 
	 * @param domainUid the domain identifier
	 * @param mbox      the {@link Mailbox} item
	 * @return conversation subtree container UID
	 */
	@GET
	@Path("{domain}/_conversation_subtree")
	public default String getConversationSubtreeUid(@PathParam("domain") String domainUid, ItemValue<Mailbox> mbox) {
		return conversationSubtreeUid(domainUid, mbox.uid);
	}

	public static String mboxRecords(@PathParam("uid") String mailboxUniqueId) {
		return MAILBOX_RECORDS_PREFIX + mailboxUniqueId;
	}

	public static String uniqueId(@PathParam("uid") String recordsContainerUid) {
		return recordsContainerUid.substring(MAILBOX_RECORDS_PREFIX.length());
	}

	public static String subtreeUid(@PathParam("domainUid") String domainUid, ItemValue<Mailbox> mbox) {
		return subtreeUid(domainUid, mbox.value.type, mbox.uid);
	}

	public static String subtreeUid(String domainUid, Mailbox.Type type, String uid) {
		return "subtree_" + domainUid.replace('.', '_') + "!" + type.nsPrefix + uid;
	}

	public static String conversationSubtreeUid(@PathParam("domainUid") String domainUid, String uid) {
		return "subtree_" + domainUid.replace('.', '_') + "!" + uid + "_conversations";
	}

}
