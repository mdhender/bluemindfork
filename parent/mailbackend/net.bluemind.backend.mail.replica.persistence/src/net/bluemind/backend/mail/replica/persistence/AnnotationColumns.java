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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class AnnotationColumns {

	private AnnotationColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("mbox")//
			.col("user_id")//
			.col("entry")//
			.col("value")//
	;

	public static EntityPopulator<MailboxAnnotation> populator() {
		return new EntityPopulator<MailboxAnnotation>() {

			@Override
			public int populate(ResultSet rs, int index, MailboxAnnotation value) throws SQLException {
				value.mailbox = rs.getString(index++);
				value.userId = rs.getString(index++);
				value.entry = rs.getString(index++);
				value.value = rs.getString(index++);
				return index;
			}
		};
	}

	public static StatementValues<MailboxAnnotation> values() {
		return new StatementValues<MailboxAnnotation>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailboxAnnotation value) throws SQLException {
				statement.setString(index++, value.mailbox);
				statement.setString(index++, value.userId);
				statement.setString(index++, value.entry);
				statement.setString(index++, value.value);
				return index;
			}
		};
	}

}
