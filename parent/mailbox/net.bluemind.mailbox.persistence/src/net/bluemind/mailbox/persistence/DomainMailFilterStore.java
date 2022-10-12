package net.bluemind.mailbox.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailbox.api.MailFilter;

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
		String query = "SELECT " + MailboxRuleColumns.cols.names() //
				+ " FROM t_domainmailfilter_rule" //
				+ " WHERE container_id = ?" //
				+ " ORDER BY type, row_idx";
		MailFilter mailFilter = new MailFilter();
		mailFilter.rules = select(query, MailboxRuleColumns.creator(), //
				MailboxRuleColumns.populator(), new Object[] { mailboxesContainer.id });
		return mailFilter;
	}
}
