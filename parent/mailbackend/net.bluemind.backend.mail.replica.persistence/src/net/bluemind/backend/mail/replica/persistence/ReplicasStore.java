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
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;

public class ReplicasStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(ReplicasStore.class);

	public ReplicasStore(DataSource pool) {
		super(pool);
	}

	public static class SubtreeLocation {
		public String subtreeContainer;
		public String boxName;
		public String partition;

		/**
		 * shared/marketing or users/tom
		 */
		public String contName;

		public String toString() {
			return subtreeContainer + " " + boxName + ", partition: " + partition + ", contName: " + contName;
		}

		public Namespace namespace() {
			return subtreeContainer.contains("!user.") ? Namespace.users : Namespace.shared;
		}

		public String imapPath(BmContext context) {
			Namespace ns = namespace();
			if (ns == Namespace.users) {

				if (!context.getSecurityContext().fromGlobalVirt()
						&& !subtreeContainer.contains("!user." + context.getSecurityContext().getSubject())) {
					String root = contName.substring(6);
					if (boxName.equals("INBOX")) {
						return "Autres utilisateurs/" + root;
					}
					return "Autres utilisateurs/" + root + "/" + boxName;
				}
				return boxName;
			} else {
				return "Dossiers partagés/" + boxName;
			}
		}
	}

	private static final String BY_UID = "SELECT cont.uid, mr.name, cont.name FROM t_mailbox_replica mr "
			+ "INNER JOIN t_container_item item ON mr.item_id = item.id "
			+ "INNER JOIN t_container cont ON cont.id = item.container_id " + "where item.uid=? ";
	private static final int SUBTREE_LEN = "subtree_".length();

	public SubtreeLocation byUniqueId(String uniqueId) throws SQLException {

		SubtreeLocation loc = unique(BY_UID, rs -> new SubtreeLocation(), (rs, index, value) -> {
			value.subtreeContainer = rs.getString(index++);
			value.boxName = rs.getString(index++);
			value.contName = rs.getString(index++);
			value.partition = value.subtreeContainer.substring(SUBTREE_LEN,
					value.subtreeContainer.indexOf('!', SUBTREE_LEN));
			return index;
		}, new Object[] { uniqueId });
		if (logger.isDebugEnabled()) {
			logger.debug("{} => {}", uniqueId, loc);
		}
		return loc;
	}

}
