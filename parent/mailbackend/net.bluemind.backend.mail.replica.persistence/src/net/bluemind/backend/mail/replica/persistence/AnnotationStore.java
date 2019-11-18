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
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class AnnotationStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(AnnotationStore.class);

	public AnnotationStore(DataSource pool) {
		super(pool);
	}

	private static final String UPSERT = "INSERT INTO t_mailbox_annotation ( " + AnnotationColumns.COLUMNS.names()
			+ ") VALUES (" + AnnotationColumns.COLUMNS.values() + ") ON CONFLICT (mbox, user_id, entry) DO UPDATE SET ("
			+ AnnotationColumns.COLUMNS.names() + ") = (" + AnnotationColumns.COLUMNS.values() + ")";

	public void store(MailboxAnnotation qr) throws SQLException {
		insert(UPSERT, qr, Arrays.asList(AnnotationColumns.values(), AnnotationColumns.values()));
		logger.info("annot {} upserted.", qr);
	}

	public void delete(MailboxAnnotation qr) throws SQLException {
		String query = "DELETE FROM t_mailbox_annotation where mbox=? AND user_id=? AND entry=?";
		delete(query, new Object[] { qr.mailbox, qr.userId, qr.entry });
		logger.info("annot {} deleted.", qr);
	}

	private static final String BY_MBOX = "SELECT " + AnnotationColumns.COLUMNS.names()
			+ " FROM t_mailbox_annotation WHERE mbox=?";

	public List<MailboxAnnotation> byMailbox(String mbox) throws SQLException {
		return select(BY_MBOX, rs -> new MailboxAnnotation(), AnnotationColumns.populator(), new Object[] { mbox });
	}

}
