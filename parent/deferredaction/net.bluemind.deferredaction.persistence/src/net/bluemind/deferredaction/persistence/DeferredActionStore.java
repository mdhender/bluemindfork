package net.bluemind.deferredaction.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.BooleanCreator;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.deferredaction.api.DeferredAction;

public class DeferredActionStore extends AbstractItemValueStore<DeferredAction> {
	private static final Logger logger = LoggerFactory.getLogger(DeferredActionStore.class);

	private static final String TABLE_NAME = "t_domain_deferredaction";

	private static final Creator<DeferredAction> CREATOR = new Creator<DeferredAction>() {
		@Override
		public DeferredAction create(ResultSet con) throws SQLException {
			return new DeferredAction();
		}
	};

	private final Container container;

	public DeferredActionStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, DeferredAction value) throws SQLException {
		if (!exists(value)) {
			StringBuilder query = new StringBuilder("INSERT INTO " + TABLE_NAME + " (");
			DeferredActionColumns.cols.appendNames(null, query);
			query.append(", item_id) VALUES (");
			DeferredActionColumns.cols.appendValues(query);
			query.append(", ?)");
			insert(query.toString(), value, DeferredActionColumns.statementValues(), new Object[] { item.id });
		}
	}

	@Override
	public void update(Item item, DeferredAction value) throws SQLException {
		StringBuilder query = new StringBuilder("UPDATE ").append(TABLE_NAME);
		query.append(" SET (").append(DeferredActionColumns.cols.names()).append(") = (");
		DeferredActionColumns.cols.appendValues(query);
		query.append(") WHERE item_id = ?");
		update(query.toString(), value, DeferredActionColumns.statementValues(), new Object[] { item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("DELETE ");
		query.append("FROM ").append(TABLE_NAME);
		query.append(" WHERE item_id = ?");
		delete(query.toString(), new Object[] { item.id });
	}

	@Override
	public DeferredAction get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ").append(DeferredActionColumns.cols.names());
		query.append(" FROM ").append(TABLE_NAME);
		query.append(" WHERE item_id = ?");
		return unique(query.toString(), CREATOR, DeferredActionColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM " + TABLE_NAME
				+ " USING t_container_item WHERE item_id = t_container_item.id AND container_id = ?",
				new Object[] { container.id });
	}

	public List<Long> getByActionId(String actionId, Date to) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT item_id");
		query.append(" FROM ").append(TABLE_NAME);
		query.append(" JOIN t_container_item ci ON (ci.id = item_id) ");
		query.append(" WHERE ").append("action_id = ?");
		query.append(" AND ").append("container_id = ?");
		query.append(" AND ").append("execution_date <= ?");
		Timestamp executionDate = new Timestamp(to.getTime());
		return select(query.toString(), LongCreator.FIRST, Collections.emptyList(),
				new Object[] { actionId, container.id, executionDate });
	}

	public boolean exists(DeferredAction value) throws SQLException {
		Timestamp executionDate = new Timestamp(value.executionDate.getTime());
		StringBuilder query = new StringBuilder("SELECT 1 FROM ").append(TABLE_NAME);
		query.append(" WHERE ").append("action_id = ? AND reference = ? and execution_date = ?");
		return unique(query.toString(), BooleanCreator.FIRST, Collections.emptyList(),
				new Object[] { value.actionId, value.reference, executionDate }) != null;
	}

	public List<Long> getByReference(String reference) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT item_id FROM ");
		query.append(TABLE_NAME);
		query.append(" WHERE reference = ?");
		return select(query.toString(), LongCreator.FIRST, Collections.emptyList(), new Object[] { reference });
	}
}
