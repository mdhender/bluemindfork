/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.icalendar.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.core.jdbc.convert.DateTimeType;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Classification;

public class ICalendarElementColumns {
	public static final Columns cols = Columns.create()//
			.col("dtstart_timestamp") //
			.col("dtstart_timezone") //
			.col("dtstart_precision", "e_datetime_precision") //
			.col("summary") //
			.col("location") //
			.col("description") //
			.col("url") //
			.col("class", "t_icalendar_class") //
			.col("status", "t_icalendar_status") //
			.col("priority") //
			.col("organizer_uri") //
			.col("organizer_cn") //
			.col("organizer_mailto") //
			.col("organizer_dir") //
			.col("exdate_timestamp") //
			.col("exdate_timezone") //
			.col("exdate_precision", "e_datetime_precision") //
			.col("rdate_timestamp") //
			.col("rdate_timezone") //
			.col("rdate_precision", "e_datetime_precision") //
			.col("attach_uri") //
			.col("attach_name") //
			.col("attach_cid") //
			.col("draft") //
			.col("sequence")//
			.col("custom_properties");

	public static StatementValues<ICalendarElement> values() {
		return new StatementValues<ICalendarElement>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow,
					ICalendarElement value) throws SQLException {

				DateTimeType.setDateTime(statement, index, value.dtstart);
				index += DateTimeType.LENGTH;
				statement.setString(index++, value.summary);
				statement.setString(index++, value.location);
				statement.setString(index++, value.description);
				statement.setString(index++, value.url);

				if (value.classification != null) {
					statement.setString(index++, value.classification.name());
				} else {
					statement.setString(index++, Classification.Public.name());
				}

				if (value.status != null) {
					statement.setString(index++, value.status.name());
				} else {
					statement.setNull(index++, Types.VARCHAR);
				}

				if (value.priority != null) {
					statement.setInt(index++, value.priority);
				} else {
					statement.setNull(index++, Types.INTEGER);
				}

				if (value.organizer != null) {
					statement.setString(index++, value.organizer.uri);
					statement.setString(index++, value.organizer.commonName);
					statement.setString(index++, value.organizer.mailto);
					statement.setString(index++, value.organizer.dir);
				} else {
					statement.setNull(index++, Types.VARCHAR);
					statement.setNull(index++, Types.VARCHAR);
					statement.setNull(index++, Types.VARCHAR);
					statement.setNull(index++, Types.VARCHAR);
				}

				DateTimeType.setDateTimeArray(conn, statement, index, value.exdate);
				index += DateTimeType.LENGTH;

				DateTimeType.setDateTimeArray(conn, statement, index, value.rdate);
				index += DateTimeType.LENGTH;

				String[] attachmentUrls = new String[value.attachments.size()];
				String[] attachmentNames = new String[value.attachments.size()];
				String[] attachmentCids = new String[value.attachments.size()];

				for (int i = 0; i < value.attachments.size(); i++) {
					attachmentUrls[i] = value.attachments.get(i).publicUrl;
					attachmentNames[i] = value.attachments.get(i).name;
					attachmentCids[i] = value.attachments.get(i).cid;
				}

				statement.setArray(index++, conn.createArrayOf("text", attachmentUrls));
				statement.setArray(index++, conn.createArrayOf("text", attachmentNames));
				statement.setArray(index++, conn.createArrayOf("text", attachmentCids));

				statement.setBoolean(index++, value.draft);

				if (value.sequence == null) {
					statement.setNull(index++, Types.INTEGER);
				} else {
					statement.setInt(index++, value.sequence);
				}

				statement.setObject(index++, value.properties);

				return index;

			}

		};
	}

	public static EntityPopulator<ICalendarElement> populator() {
		return new EntityPopulator<ICalendarElement>() {

			@Override
			public int populate(ResultSet rs, int index, ICalendarElement value) throws SQLException {

				value.dtstart = DateTimeType.getDateTime(rs, index);
				index += DateTimeType.LENGTH;

				value.summary = rs.getString(index++);
				value.location = rs.getString(index++);
				value.description = rs.getString(index++);
				value.url = rs.getString(index++);

				String classification = rs.getString(index++);
				if (classification != null) {
					value.classification = ICalendarElement.Classification.valueOf(classification);
				}

				String status = rs.getString(index++);
				if (status != null) {
					value.status = ICalendarElement.Status.valueOf(status);
				}

				String priority = rs.getString(index++);
				if (priority != null) {
					value.priority = Integer.parseInt(priority);
				}

				ICalendarElement.Organizer organizer = new ICalendarElement.Organizer();

				organizer.uri = rs.getString(index++);
				organizer.commonName = rs.getString(index++);
				organizer.mailto = rs.getString(index++);
				organizer.dir = rs.getString(index++);

				if (organizer.uri != null || organizer.mailto != null) {
					value.organizer = organizer;
				}

				value.exdate = DateTimeType.getDateTimes(rs, index);
				index += DateTimeType.LENGTH;

				value.rdate = DateTimeType.getDateTimes(rs, index);
				index += DateTimeType.LENGTH;

				String[] attachmentUrls = arrayOfString(rs.getArray(index++));
				String[] attachmentNames = arrayOfString(rs.getArray(index++));
				String[] attachmentCids = arrayOfString(rs.getArray(index++));

				List<AttachedFile> attachments = new ArrayList<>(attachmentUrls.length);
				for (int i = 0; i < attachmentUrls.length; i++) {
					AttachedFile file = new AttachedFile();
					file.publicUrl = attachmentUrls[i];
					file.name = attachmentNames[i];
					file.expirationDate = 0l;
					file.cid = attachmentCids[i];
					attachments.add(file);
				}
				value.attachments = attachments;

				value.draft = rs.getBoolean(index++);

				value.sequence = rs.getInt(index++);

				@SuppressWarnings("unchecked")
				Map<String, String> properties = (Map<String, String>) rs.getObject(index++);
				if (properties != null) {
					value.properties.putAll(properties);
				}

				return index;
			}

		};

	}

	protected static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];
		}
		return ret;
	}
}
