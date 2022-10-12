package net.bluemind.mailbox.persistence;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class MailFilterStore extends JdbcAbstractStore {

	private final Container container;

	public MailFilterStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
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

	public List<String> findOutOfOffice(Date date) throws SQLException {
		String query = "SELECT item.uid FROM t_mailfilter_vacation v, t_container_item item WHERE item.container_id = ? "
				+ "AND v.item_id = item.id  AND v.vacation_marker = false AND v.active = true AND v.start_date <= ? AND v.end_date > ? ";
		return select(query, new StringCreator(1), Collections.emptyList(), new Object[] { container.id,
				new java.sql.Timestamp(date.getTime()), new java.sql.Timestamp(date.getTime()) });
	}

	public List<String> findInOffice(Date date) throws SQLException {
		String query = "SELECT item.uid FROM t_mailfilter_vacation v, t_container_item item WHERE item.container_id = ? "
				+ "AND v.item_id = item.id AND v.vacation_marker = true AND NOT (v.start_date <= ? AND v.end_date > ? )";
		return select(query, new StringCreator(1), Collections.emptyList(), new Object[] { container.id,
				new java.sql.Timestamp(date.getTime()), new java.sql.Timestamp(date.getTime()) });
	}

	public void markOutOfOffice(Item item, boolean activated) throws SQLException {
		String query = "UPDATE t_mailfilter_vacation set vacation_marker = ? WHERE item_id = ?";
		update(query, null, new Object[] { activated, item.id });
	}

}
