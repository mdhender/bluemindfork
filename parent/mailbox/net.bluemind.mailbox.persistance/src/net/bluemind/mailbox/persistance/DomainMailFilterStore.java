package net.bluemind.mailbox.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailbox.api.MailFilter;

public class DomainMailFilterStore extends JdbcAbstractStore {

	private static final Creator<MailFilter.Rule> FILTER_CREATOR = new Creator<MailFilter.Rule>() {

		@Override
		public MailFilter.Rule create(ResultSet con) throws SQLException {
			return new MailFilter.Rule();
		}
	};

	private Container mailboxesContainer;

	public DomainMailFilterStore(DataSource dataSource, Container mailboxesContainer) {
		super(dataSource);
		this.mailboxesContainer = mailboxesContainer;
	}

	public void set(MailFilter value) throws SQLException {
		delete();

		if (value.rules.size() == 0) {
			return;
		}

		String query = "INSERT INTO t_domainmailfilter_rule (" + MailFilterRuleColumns.cols.names() + ", container_id " //
				+ " )" + " VALUES (" //
				+ MailFilterRuleColumns.cols.values() + ", ? )";

		batchInsert(query, value.rules, MailFilterRuleColumns.statementValues(mailboxesContainer.id));
	}

	public void delete() throws SQLException {
		delete("DELETE FROM t_domainmailfilter_rule WHERE container_id = ?", new Object[] { mailboxesContainer.id });
	}

	public MailFilter get() throws SQLException {
		String query = "SELECT " + MailFilterRuleColumns.cols.names()
				+ " FROM t_domainmailfilter_rule WHERE container_id = ?";

		List<MailFilter.Rule> rules = select(query, FILTER_CREATOR, MailFilterRuleColumns.populator(),
				new Object[] { mailboxesContainer.id });

		MailFilter s = new MailFilter();
		s.rules = rules;
		return s;
	}
}
