package net.bluemind.mailbox.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.RuleMoveDirection;
import net.bluemind.mailbox.api.rules.RuleMoveRelativePosition;

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
		String query = String.format("""
				SELECT id, %s
				FROM t_mailfilter_rule WHERE item_id = ?
				ORDER BY type, row_idx""", MailboxRuleColumns.cols.names());
		s.rules = select(query, MailboxRuleColumns.creator(), //
				MailboxRuleColumns.populator(), new Object[] { item.id });
		s.vacation = new Vacation();
		s.forwarding = new Forwarding();
		return s;
	}

	public MailFilterRule getRule(Item item, long id) throws SQLException {
		String query = String.format("""
				SELECT id, %s
				FROM t_mailfilter_rule
				WHERE item_id = ? AND id = ?
				ORDER BY type, row_idx""", MailboxRuleColumns.cols.names());
		return unique(query, MailboxRuleColumns.creator(), //
				MailboxRuleColumns.populator(), new Object[] { item.id, id });
	}

	public long addRule(Item item, MailFilterRule value) throws SQLException {
		String query = """
				WITH
				anchor AS (SELECT max(row_idx) as row_idx FROM t_mailfilter_rule WHERE item_id = ? AND client = ? AND type = ?::enum_mailbox_rule_type),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx + 1
				 WHERE item_id = ? AND row_idx > (SELECT row_idx FROM anchor) RETURNING row_idx)
				INSERT INTO t_mailfilter_rule (%s, item_id)
				 SELECT ?, ?::enum_mailbox_rule_type, ?::enum_mailbox_rule_trigger, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, COALESCE((SELECT row_idx + 1 FROM anchor), max(row_idx) + 1, 1), ?
				 FROM t_mailfilter_rule WHERE item_id = ?
				""";
		query = String.format(query, MailboxRuleColumns.cols.names());
		return insertWithSerial(query,
				new Object[] { item.id, value.client, value.type.name(), item.id, value.client, value.type.name(),
						value.trigger.name(), value.active, value.deferred, value.name,
						JsonUtils.asString(value.clientProperties), JsonUtils.asString(value.conditions),
						JsonUtils.asString(value.actions), value.stop, item.id, item.id });
	}

	public long addRule(Item item, RuleMoveRelativePosition position, long anchorId, MailFilterRule value)
			throws SQLException {
		int shiftAnchor = (position == RuleMoveRelativePosition.AFTER) ? 0 : 1;
		String query = """
				WITH
				anchor AS (SELECT row_idx, type, client FROM t_mailfilter_rule WHERE id = ?),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx + 1
				 WHERE item_id = ? AND row_idx + ? > (SELECT row_idx FROM anchor) RETURNING row_idx),
				pos AS (SELECT min(row_idx) - 1 as row_idx FROM updated)
				INSERT INTO t_mailfilter_rule (%s, item_id)
				 SELECT ?, ?::enum_mailbox_rule_type, ?::enum_mailbox_rule_trigger, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, COALESCE(max((SELECT row_idx FROM pos)), (SELECT row_idx + 1 FROM anchor)), ?
				 FROM t_mailfilter_rule WHERE item_id = ?
				""";
		query = String.format(query, MailboxRuleColumns.cols.names());
		return insertWithSerial(query,
				new Object[] { anchorId, item.id, shiftAnchor, value.client, value.type.name(), value.trigger.name(),
						value.active, value.deferred, value.name, JsonUtils.asString(value.clientProperties),
						JsonUtils.asString(value.conditions), JsonUtils.asString(value.actions), value.stop, item.id,
						item.id });
	}

	public void updateRule(Item item, long id, MailFilterRule value) throws SQLException {
		String query = "UPDATE t_mailfilter_rule "
				+ "SET (client, type, trigger, active, deferred_action, name, client_properties, conditions, actions, stop, item_id) = "
				+ "(?, ?::enum_mailbox_rule_type, ?::enum_mailbox_rule_trigger, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?) "
				+ "WHERE id = ?";
		update(query,
				new Object[] { value.client, value.type.name(), value.trigger.name(), value.active, value.deferred,
						value.name, JsonUtils.asString(value.clientProperties), JsonUtils.asString(value.conditions),
						JsonUtils.asString(value.actions), value.stop, item.id, id });
	}

	public void deleteRule(Item item, long id) throws SQLException {
		String query = "DELETE FROM t_mailfilter_rule WHERE item_id = ? AND id = ?";
		delete(query, new Object[] { item.id, id });
	}

	public void moveRule(Item item, long id, RuleMoveRelativePosition position, long anchorId) throws SQLException {
		int shiftAnchorWhenMovedDown = (position == RuleMoveRelativePosition.AFTER) ? 1 : 0;
		int shiftAnchorWhenMovedUp = (position == RuleMoveRelativePosition.AFTER) ? 0 : 1;
		String query = """
				WITH
				anchor AS (SELECT row_idx, type, client FROM t_mailfilter_rule WHERE id = ?),
				moved AS (SELECT row_idx, type, client FROM t_mailfilter_rule WHERE id = ?),
				dir AS (SELECT CASE
				        WHEN moved.row_idx - anchor.row_idx < 0 THEN -1
				        ELSE 1
				        END
				        FROM anchor, moved),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx + (SELECT dir.case FROM dir)
				      WHERE (item_id = ? AND type = (SELECT type FROM moved) AND client = (SELECT client FROM moved))
				      AND ((-1 = (SELECT dir.case FROM dir) AND row_idx - ? < (SELECT row_idx FROM anchor) AND row_idx > (SELECT row_idx FROM moved))
				       OR (1 = (SELECT dir.case FROM dir) AND row_idx < (SELECT row_idx FROM moved) AND row_idx + ? > (SELECT row_idx FROM anchor)))
				      RETURNING row_idx),
				pos AS (SELECT CASE
				      WHEN (SELECT dir.case FROM dir) = -1 THEN max(row_idx) + 1
				      ELSE min(row_idx) - 1
				      END
				   FROM updated)
				UPDATE t_mailfilter_rule SET row_idx = (SELECT pos.case FROM pos)
				 WHERE item_id = ? AND id = ? AND (SELECT pos.case FROM pos) IS NOT NULL
				""";
		update(query,
				new Object[] { anchorId, id, item.id, shiftAnchorWhenMovedDown, shiftAnchorWhenMovedUp, item.id, id });
	}

	public void moveRule(Item item, long id, RuleMoveDirection direction) throws SQLException {
		switch (direction) {
		case TOP:
			moveRuleToTop(item, id);
			break;
		case UP:
			moveRuleUp(item, id);
			break;
		case BOTTOM:
			moveRuleToBottom(item, id);
			break;
		case DOWN:
			moveRuleDown(item, id);
			break;
		}
	}

	private void moveRuleToTop(Item item, long id) throws SQLException {
		String query = """
				WITH
				moved AS (SELECT row_idx, type, client FROM t_mailfilter_rule WHERE id = ?),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx + 1
				 WHERE item_id = ? AND type = (SELECT type FROM moved) AND client = (SELECT client FROM moved)
				   AND row_idx < (SELECT row_idx FROM moved)
				 RETURNING row_idx),
				pos AS (SELECT min(row_idx) as row_idx FROM updated)
				UPDATE t_mailfilter_rule SET row_idx = (SELECT row_idx - 1 FROM pos)
				 WHERE item_id = ? AND id = ? AND (SELECT row_idx FROM pos) IS NOT NULL
				""";
		update(query, new Object[] { id, item.id, item.id, id });
	}

	private void moveRuleUp(Item item, long id) throws SQLException {
		String query = """
				WITH
				moved AS (SELECT row_idx, client, type FROM t_mailfilter_rule WHERE id = ?),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx + 1
				 WHERE item_id = ? AND type = (SELECT type FROM moved) AND client = (SELECT client FROM moved)
				   AND row_idx = (SELECT max(a.row_idx) FROM t_mailfilter_rule as a WHERE a.row_idx < (SELECT moved.row_idx FROM moved))
				 RETURNING row_idx),
				pos AS (SELECT min(row_idx) as row_idx FROM updated)
				UPDATE t_mailfilter_rule SET row_idx = (SELECT row_idx - 1 FROM pos)
				 WHERE item_id = ? AND id = ? AND (SELECT row_idx FROM pos) IS NOT NULL
				""";
		update(query, new Object[] { id, item.id, item.id, id });
	}

	private void moveRuleToBottom(Item item, long id) throws SQLException {
		String query = """
				WITH
				moved AS (SELECT row_idx, type, client FROM t_mailfilter_rule WHERE id = ?),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx - 1
				 WHERE item_id = ? AND type = (SELECT type FROM moved) AND client = (SELECT client FROM moved)
				   AND row_idx > (SELECT row_idx FROM moved)
				 RETURNING row_idx),
				pos AS (SELECT max(row_idx) as row_idx FROM updated)
				UPDATE t_mailfilter_rule SET row_idx = (SELECT row_idx + 1 FROM pos)
				 WHERE item_id = ? AND id = ? AND (SELECT row_idx FROM pos) IS NOT NULL
				""";
		update(query, new Object[] { id, item.id, item.id, id });
	}

	private void moveRuleDown(Item item, long id) throws SQLException {
		String query = """
				WITH
				moved AS (SELECT row_idx, type, client FROM t_mailfilter_rule WHERE id = ?),
				updated AS (UPDATE t_mailfilter_rule SET row_idx = row_idx - 1
				 WHERE item_id = ? AND type = (SELECT type FROM moved) AND client = (SELECT client FROM moved)
				   AND row_idx = (SELECT min(a.row_idx) FROM t_mailfilter_rule as a WHERE a.row_idx > (SELECT row_idx FROM moved))
				 RETURNING row_idx),
				pos AS (SELECT max(row_idx) as row_idx FROM updated)
				UPDATE t_mailfilter_rule SET row_idx = (SELECT row_idx + 1 FROM pos)
				 WHERE item_id = ? AND id = ? AND (SELECT row_idx FROM pos) IS NOT NULL
				""";
		update(query, new Object[] { id, item.id, item.id, id });
	}

}
