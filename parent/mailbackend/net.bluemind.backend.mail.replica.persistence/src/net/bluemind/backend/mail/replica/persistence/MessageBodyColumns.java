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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ListReader;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class MessageBodyColumns {

	private MessageBodyColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("subject")//
			.col("structure", "jsonb") //
			.col("headers", "jsonb")//
			.col("recipients", "jsonb")//
			.col("message_id")//
			.col("references_header")//
			.col("date_header")//
			.col("size")//
			.col("preview")//
			.col("body_version");

	private static final ListReader<Header> headersReader = JsonUtils.listReader(Header.class);
	private static final ListReader<Recipient> recipientReader = JsonUtils.listReader(Recipient.class);
	private static final ValueReader<Part> partReader = JsonUtils.reader(Part.class);

	public static EntityPopulator<MessageBody> populator(String guid) {
		final EntityPopulator<MessageBody> simple = simplePopulator();
		return (ResultSet rs, int index, MessageBody value) -> {
			value.guid = guid;
			return simple.populate(rs, index, value);
		};

	}

	public static EntityPopulator<MessageBody> simplePopulator() {
		return (ResultSet rs, int index, MessageBody value) -> {
			value.subject = rs.getString(index++);
			value.structure = partReader.read(rs.getString(index++));
			value.headers = headersReader.read(rs.getString(index++));
			value.recipients = recipientReader.read(rs.getString(index++));
			value.messageId = rs.getString(index++);
			value.references = toList(rs.getArray(index++));
			value.date = rs.getTimestamp(index++);
			value.size = rs.getInt(index++);
			value.preview = rs.getString(index++);
			value.bodyVersion = rs.getInt(index++);
			value.smartAttach = value.structure.hasRealAttachments();
			return index;
		};
	}

	private static List<String> toList(Array array) throws SQLException {
		if (array == null)
			return null;
		String[] ret = (String[]) array.getArray();
		return Arrays.asList(ret);
	}

	public static StatementValues<MessageBody> values(String guid) {
		return new StatementValues<MessageBody>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MessageBody value) throws SQLException {
				statement.setString(index++, value.subject);
				statement.setString(index++, JsonUtils.asString(value.structure));
				statement.setString(index++, JsonUtils.asString(value.headers));
				statement.setString(index++, JsonUtils.asString(value.recipients));
				statement.setString(index++, value.messageId);
				statement.setArray(index++,
						con.createArrayOf("text", value.references == null ? null : value.references.toArray()));
				statement.setTimestamp(index++, new Timestamp(value.date == null ? 0 : value.date.getTime()));
				statement.setInt(index++, value.size);
				statement.setString(index++, value.preview);
				statement.setInt(index++, value.bodyVersion);

				if (guid != null) {
					statement.setString(index++, guid);
				}
				return index;
			}

		};
	}

}
