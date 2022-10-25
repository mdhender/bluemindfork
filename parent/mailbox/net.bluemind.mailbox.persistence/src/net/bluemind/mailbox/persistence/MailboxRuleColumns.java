package net.bluemind.mailbox.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleAction;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;

public final class MailboxRuleColumns {

	public static final Columns cols = Columns.create() //
			.col("client") //
			.col("type", "enum_mailbox_rule_type") //
			.col("trigger", "enum_mailbox_rule_trigger") //
			.col("active") //
			.col("deferred_action") //
			.col("name") //
			.col("client_properties", "jsonb") //
			.col("conditions", "jsonb") //
			.col("actions", "jsonb") //
			.col("stop") //
			.col("row_idx");

	private MailboxRuleColumns() {

	}

	public static Creator<MailFilterRule> creator() {
		return new Creator<MailFilterRule>() {

			@Override
			public MailFilterRule create(ResultSet con) throws SQLException {
				return new MailFilterRule();
			}
		};
	}

	public static JdbcAbstractStore.StatementValues<MailFilterRule> statementValues(final long itemId) {
		return new JdbcAbstractStore.StatementValues<MailFilterRule>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailFilterRule value) throws SQLException {
				statement.setString(index++, value.client);
				statement.setString(index++, value.type.name());
				statement.setString(index++, value.trigger.name());
				statement.setBoolean(index++, value.active);
				statement.setBoolean(index++, value.deferred);
				statement.setString(index++, value.name);
				statement.setString(index++, JsonUtils.asString(value.clientProperties));
				statement.setString(index++, JsonUtils.asString(value.conditions));
				statement.setString(index++, JsonUtils.asString(value.actions));
				statement.setBoolean(index++, value.stop);
				statement.setInt(index++, currentRow);
				statement.setLong(index++, itemId);
				return index;
			}
		};
	}

	public static JdbcAbstractStore.EntityPopulator<MailFilterRule> populator() {
		return new JdbcAbstractStore.EntityPopulator<MailFilterRule>() {

			@Override
			public int populate(ResultSet rs, int index, MailFilterRule value) throws SQLException {
				value.client = rs.getString(index++);
				value.type = MailFilterRule.Type.valueOf(rs.getString(index++));
				value.trigger = MailFilterRule.Trigger.valueOf(rs.getString(index++));
				value.active = rs.getBoolean(index++);
				value.deferred = rs.getBoolean(index++);
				value.name = rs.getString(index++);
				value.clientProperties = JsonUtils.readMap(rs.getString(index++), String.class, String.class);
				value.conditions = JsonUtils.readSome(rs.getString(index++), MailFilterRuleCondition.class);
				value.actions = JsonUtils.readSome(rs.getString(index++), MailFilterRuleAction.class);
				value.stop = rs.getBoolean(index++);
				index++; // discard row_index
				return index;
			}

		};
	}

}
