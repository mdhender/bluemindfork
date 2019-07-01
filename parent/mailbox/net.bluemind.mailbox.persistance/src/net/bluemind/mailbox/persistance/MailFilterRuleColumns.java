package net.bluemind.mailbox.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.mailbox.api.MailFilter;

public final class MailFilterRuleColumns {

	public static final Columns cols = Columns.create() //
			.col("criteria") //
			.col("star") //
			.col("mark_read") //
			.col("delete_it") //
			.col("forward") //
			.col("forward_with_copy") //
			.col("deliver") //
			.col("discard") //
			.col("row_idx") //
			.col("active");

	public static Creator<MailFilter.Rule> creator() {
		return new Creator<MailFilter.Rule>() {

			@Override
			public MailFilter.Rule create(ResultSet con) throws SQLException {
				return new MailFilter.Rule();
			}
		};
	}

	public static MailFilterStore.StatementValues<MailFilter.Rule> statementValues(final long itemId) {
		return new JdbcAbstractStore.StatementValues<MailFilter.Rule>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailFilter.Rule value) throws SQLException {
				statement.setString(index++, value.criteria);
				statement.setBoolean(index++, value.star);
				statement.setBoolean(index++, value.read);
				statement.setBoolean(index++, value.delete);
				statement.setString(index++,
						value.forward.emails.isEmpty() ? null : String.join(",", value.forward.emails));
				statement.setBoolean(index++, value.forward.localCopy);
				statement.setString(index++, value.deliver);
				statement.setBoolean(index++, value.discard);
				statement.setInt(index++, currentRow);
				statement.setBoolean(index++, value.active);

				statement.setLong(index++, itemId);
				return index;
			}
		};
	}

	public static MailFilterStore.EntityPopulator<MailFilter.Rule> populator() {
		return new JdbcAbstractStore.EntityPopulator<MailFilter.Rule>() {

			@Override
			public int populate(ResultSet rs, int index, MailFilter.Rule value) throws SQLException {
				value.criteria = rs.getString(index++);
				value.star = rs.getBoolean(index++);
				value.read = rs.getBoolean(index++);
				value.delete = rs.getBoolean(index++);

				String forwardTo = rs.getString(index++);
				if (forwardTo != null) {
					for (String e : forwardTo.split(",")) {
						e = e.trim();
						if (!e.isEmpty()) {
							value.forward.emails.add(e);
						}
					}
				}

				value.forward.localCopy = rs.getBoolean(index++);

				value.deliver = rs.getString(index++);
				value.discard = rs.getBoolean(index++);
				index++; // discard row_index
				value.active = rs.getBoolean(index++);
				return index;
			}

		};
	}

}
