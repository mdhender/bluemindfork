package net.bluemind.mailbox.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.mailbox.api.MailFilter;

public final class MailFilterForwardingColumns {

	public static final Columns cols = Columns.create() //
			.col("active") //
			.col("local_copy") //
			.col("email");

	public static Creator<MailFilter.Forwarding> creator() {
		return new Creator<MailFilter.Forwarding>() {

			@Override
			public MailFilter.Forwarding create(ResultSet con) throws SQLException {
				return new MailFilter.Forwarding();
			}
		};
	}

	public static MailFilterStore.StatementValues<MailFilter.Forwarding> statementValues(final long itemId) {
		return new JdbcAbstractStore.StatementValues<MailFilter.Forwarding>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailFilter.Forwarding value) throws SQLException {
				statement.setBoolean(index++, value.enabled);
				statement.setBoolean(index++, value.localCopy);
				statement.setString(index++, value.emails.isEmpty() ? null : String.join(",", value.emails));
				statement.setLong(index++, itemId);
				return index;
			}
		};
	}

	public static MailFilterStore.EntityPopulator<MailFilter.Forwarding> populator() {
		return new JdbcAbstractStore.EntityPopulator<MailFilter.Forwarding>() {

			@Override
			public int populate(ResultSet rs, int index, MailFilter.Forwarding value) throws SQLException {
				value.enabled = rs.getBoolean(index++);
				value.localCopy = rs.getBoolean(index++);

				String forwardTo = rs.getString(index++);
				if (forwardTo != null) {
					for (String e : forwardTo.split(",")) {
						e = e.trim();
						if (!e.isEmpty()) {
							value.emails.add(e);
						}
					}
				}

				return index;
			}

		};
	}

}
