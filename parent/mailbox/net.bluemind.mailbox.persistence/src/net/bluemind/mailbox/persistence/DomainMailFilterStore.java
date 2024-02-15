package net.bluemind.mailbox.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.rules.MailFilterRule;

public class DomainMailFilterStore extends JdbcAbstractStore {

	private final Container mailboxesContainer;

	public DomainMailFilterStore(DataSource dataSource, Container mailboxesContainer) {
		super(dataSource);
		this.mailboxesContainer = mailboxesContainer;
	}

	public void set(MailFilter value) throws SQLException {
		delete();
		if (!value.rules.isEmpty()) {
			String query = String.format("INSERT INTO t_domainmailfilter_rule (%s, container_id) VALUES (%s, ?)", //
					MailboxRuleColumns.cols.names(), MailboxRuleColumns.cols.values());
			batchInsert(query, value.rules, MailboxRuleColumns.statementValues(mailboxesContainer.id));
		}
	}

	public void delete() throws SQLException {
		delete("DELETE FROM t_domainmailfilter_rule WHERE container_id = ?", new Object[] { mailboxesContainer.id });
	}

	public MailFilter get() throws SQLException {
		String query = "SELECT id, " + MailboxRuleColumns.cols.names() //
				+ " FROM t_domainmailfilter_rule" //
				+ " WHERE container_id = ?" //
				+ " ORDER BY type, row_idx";
		MailFilter mailFilter = new MailFilter();
		mailFilter.rules = select(query, MailboxRuleColumns.creator(), //
				MailboxRuleColumns.populator(), new Object[] { mailboxesContainer.id });
		return mailFilter;
	}

	public MailFilterRule getRule(long id) throws SQLException {
		String query = "SELECT id, " + MailboxRuleColumns.cols.names() //
				+ " FROM t_domainmailfilter_rule" //
				+ " WHERE container_id = ? AND id = ?" //
				+ " ORDER BY type, row_idx";
		return unique(query, MailboxRuleColumns.creator(), //
				MailboxRuleColumns.populator(), new Object[] { mailboxesContainer.id, id });
	}

	public long addRule(MailFilterRule value) throws SQLException {
		String query = String.format("INSERT INTO t_domainmailfilter_rule (%s, container_id) " //
				+ "SELECT ?, ?::enum_mailbox_rule_type, ?::enum_mailbox_rule_trigger, ?, ?, ?, "
				+ "?::jsonb, ?::jsonb, ?::jsonb, ?, COALESCE(max(row_idx)+1, 1), ? " //
				+ "FROM t_domainmailfilter_rule WHERE container_id = ?", //
				MailboxRuleColumns.cols.names());

		return insertWithSerial(query,
				new Object[] { value.client, value.type.name(), value.trigger.name(), value.active, value.deferred,
						value.name, JsonUtils.asString(value.clientProperties), JsonUtils.asString(value.conditions),
						JsonUtils.asString(value.actions), value.stop, mailboxesContainer.id, mailboxesContainer.id });
	}

	public void updateRule(long id, MailFilterRule value) throws SQLException {
		String query = String.format("UPDATE t_domainmailfilter_rule set (%s, container_id) = (%s, ?) WHERE id = ?", //
				MailboxRuleColumns.cols.names(), MailboxRuleColumns.cols.values());
		update(query, value, MailboxRuleColumns.statementValues(mailboxesContainer.id), new Object[] { id });
	}

	public void deleteRule(long id) throws SQLException {
		String query = "DELETE FROM t_domainmailfilter_rule WHERE container_id = ? AND id = ?";
		delete(query, new Object[] { mailboxesContainer.id, id });
	}
}
