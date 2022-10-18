package net.bluemind.mailbox.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class MailFilterStore extends JdbcAbstractStore {

	public MailFilterStore(DataSource dataSource) {
		super(dataSource);
	}

	public void set(Item item, MailFilter value) throws SQLException {
		delete(item);

		if (value.rules.isEmpty()) {
			return;
		}
		String query = String.format("INSERT INTO t_mailfilter_rule (%s, item_id) VALUES (%s, ?)", //
				MailboxRuleColumns.cols.names(), MailboxRuleColumns.cols.values());
		batchInsert(query, value.rules, MailboxRuleColumns.statementValues(item.id));
	}

	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_mailfilter_rule WHERE item_id = ?", new Object[] { item.id });
	}

	public MailFilter get(Item item) throws SQLException {
		MailFilter s = new MailFilter();
		String query = "SELECT " + MailboxRuleColumns.cols.names() //
				+ " FROM t_mailfilter_rule WHERE item_id = ?" //
				+ " ORDER BY type, row_idx";
		s.rules = select(query, MailboxRuleColumns.creator(), //
				MailboxRuleColumns.populator(), new Object[] { item.id });
		s.vacation = new Vacation();
		s.forwarding = new Forwarding();
		return s;
	}

}
