package net.bluemind.mailbox.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.core.jdbc.convert.DateTimeType;
import net.bluemind.mailbox.api.MailFilter;

public final class MailFilterVacationColumns {

	public static final Columns cols = Columns.create() //
			.col("active") //
			.col("start_date") //
			.col("end_date") //
			.col("subject") //
			.col("body");

	public static Creator<MailFilter.Vacation> creator() {
		return new Creator<MailFilter.Vacation>() {

			@Override
			public MailFilter.Vacation create(ResultSet con) throws SQLException {
				return new MailFilter.Vacation();
			}
		};
	}

	public static MailFilterStore.StatementValues<MailFilter.Vacation> statementValues(final long itemId) {
		return new JdbcAbstractStore.StatementValues<MailFilter.Vacation>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailFilter.Vacation value) throws SQLException {
				statement.setBoolean(index++, value.enabled);

				if (value.start != null) {
					statement.setTimestamp(index++, DateTimeType.asTimestamp(value.start));
				} else {
					statement.setNull(index++, Types.TIMESTAMP);
				}

				if (value.end != null) {
					statement.setTimestamp(index++, DateTimeType.asTimestamp(value.end));
				} else {
					statement.setNull(index++, Types.TIMESTAMP);
				}

				statement.setString(index++, value.subject);
				statement.setString(index++, value.text);
				statement.setLong(index++, itemId);
				return index;
			}
		};
	}

	public static MailFilterStore.EntityPopulator<MailFilter.Vacation> populator() {
		return new JdbcAbstractStore.EntityPopulator<MailFilter.Vacation>() {

			@Override
			public int populate(ResultSet rs, int index, MailFilter.Vacation value) throws SQLException {
				value.enabled = rs.getBoolean(index++);
				Timestamp start = rs.getTimestamp(index++);
				if (start != null) {
					value.start = DateTimeType.fromTimestamp(start, "UTC", BmDateTime.Precision.DateTime.name());
				}

				Timestamp end = rs.getTimestamp(index++);
				if (end != null) {
					value.end = DateTimeType.fromTimestamp(end, "UTC", BmDateTime.Precision.DateTime.name());
				}

				value.subject = rs.getString(index++);
				value.text = rs.getString(index++);
				return index;
			}

		};
	}

}
