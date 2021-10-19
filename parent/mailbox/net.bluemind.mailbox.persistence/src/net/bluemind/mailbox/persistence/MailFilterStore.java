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
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class MailFilterStore extends JdbcAbstractStore {

	private Container container;

	public MailFilterStore(DataSource dataSource, Container container) {
		super(dataSource);
		this.container = container;
	}

	public void set(Item item, MailFilter value) throws SQLException {
		delete(item);
		insertRules(item, value.rules);
		insertVacation(item, value.vacation);
		insertForwarding(item, value.forwarding);
	}

	private void insertForwarding(Item item, Forwarding forwarding) throws SQLException {

		if (forwarding == null) {
			return;
		}

		String query = "INSERT INTO t_mailfilter_forwarding (" + MailFilterForwardingColumns.cols.names() + ", item_id " //
				+ " )" + " VALUES (" //
				+ MailFilterForwardingColumns.cols.values() + ", ? )";

		insert(query, forwarding, MailFilterForwardingColumns.statementValues(item.id));

	}

	private void insertVacation(Item item, Vacation vacation) throws SQLException {

		if (vacation == null) {
			return;
		}
		String query = "INSERT INTO t_mailfilter_vacation (" + MailFilterVacationColumns.cols.names() + ", item_id " //
				+ " )" + " VALUES (" //
				+ MailFilterVacationColumns.cols.values() + ", ? )";

		insert(query, vacation, MailFilterVacationColumns.statementValues(item.id));

	}

	private void insertRules(Item item, List<Rule> rules) throws SQLException {
		if (rules.size() == 0) {
			return;
		}

		String query = "INSERT INTO t_mailfilter_rule (" + MailFilterRuleColumns.cols.names() + ", item_id " //
				+ " )" + " VALUES (" //
				+ MailFilterRuleColumns.cols.values() + ", ? )";

		batchInsert(query, rules, MailFilterRuleColumns.statementValues(item.id));

	}

	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_mailfilter_rule WHERE item_id = ?", new Object[] { item.id });

		delete("DELETE FROM t_mailfilter_vacation WHERE item_id = ?", new Object[] { item.id });

		delete("DELETE FROM t_mailfilter_forwarding WHERE item_id = ?", new Object[] { item.id });
	}

	public MailFilter get(Item item) throws SQLException {
		MailFilter s = new MailFilter();

		s.rules = selectRules(item);
		s.vacation = selectVacation(item);
		s.forwarding = selectForwarding(item);
		return s;
	}

	private Forwarding selectForwarding(Item item) throws SQLException {
		String query = "SELECT " + MailFilterForwardingColumns.cols.names()
				+ " FROM t_mailfilter_forwarding WHERE item_id = ?";
		Forwarding forwarding = unique(query, MailFilterForwardingColumns.creator(),
				MailFilterForwardingColumns.populator(), new Object[] { item.id });

		return (forwarding == null) ? new Forwarding() : forwarding;
	}

	private Vacation selectVacation(Item item) throws SQLException {
		String query = "SELECT " + MailFilterVacationColumns.cols.names()
				+ " FROM t_mailfilter_vacation WHERE item_id = ?";
		Vacation vacation = unique(query, MailFilterVacationColumns.creator(), MailFilterVacationColumns.populator(),
				new Object[] { item.id });

		return (vacation == null) ? new Vacation() : vacation;
	}

	private List<Rule> selectRules(Item item) throws SQLException {
		String query = "SELECT " + MailFilterRuleColumns.cols.names()
				+ " FROM t_mailfilter_rule WHERE item_id = ? ORDER BY row_idx";

		return select(query, MailFilterRuleColumns.creator(), MailFilterRuleColumns.populator(),
				new Object[] { item.id });

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
