package net.bluemind.user.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.user.api.User;

public class UserStore extends AbstractItemValueStore<User> {

	private static final Logger logger = LoggerFactory.getLogger(UserStore.class);

	private final Container container;

	private static final Creator<User> USER_CREATOR = new Creator<User>() {
		@Override
		public User create(ResultSet con) throws SQLException {
			return new User();
		}
	};

	private static final Creator<Integer> INTEGER_CREATOR = new Creator<Integer>() {
		@Override
		public Integer create(ResultSet con) throws SQLException {
			return con.getInt(1);
		}
	};

	private static final Creator<String> STRING_CREATOR = new Creator<String>() {
		@Override
		public String create(ResultSet con) throws SQLException {
			return con.getString(1);
		}
	};

	private static final Creator<String> UIDFOUND_CREATOR = new Creator<String>() {
		@Override
		public String create(ResultSet con) throws SQLException {
			return con.getString(1);
		}
	};

	public UserStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
		logger.debug("created {}", this.container);
	}

	@Override
	public void create(Item item, User value) throws SQLException {
		StringBuilder query = new StringBuilder("INSERT INTO t_domain_user (item_id, ");
		UserColumns.cols.appendNames(null, query);
		query.append(") VALUES (" + item.id + ", ");
		UserColumns.cols.appendValues(query);
		query.append(")");
		insert(query.toString(), value, UserColumns.statementValues());
		logger.info("insert complete: {}", value);
	}

	@Override
	public void update(Item item, User value) throws SQLException {
		StringBuilder query = new StringBuilder("UPDATE t_domain_user SET (");

		UserColumns.cols.appendNames(null, query);
		query.append(") = (");
		UserColumns.cols.appendValues(query);
		query.append(") WHERE item_id = ").append(item.id);

		update(query.toString(), value, UserColumns.statementValues());
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_domain_user WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public User get(Item item) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");

		UserColumns.cols.appendNames("u", query);

		query.append(" FROM t_domain_user u");
		query.append(" INNER JOIN t_container_item item ON item_id = item.id");
		query.append(" WHERE item_id = ").append(item.id);
		User u = unique(query.toString(), USER_CREATOR, UserColumns.populator());
		if (u == null) {
			return null;
		}

		return u;
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_domain_user WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public String byLogin(String login) throws SQLException {

		String query = "SELECT item.uid FROM t_domain_user u" + " INNER JOIN t_container_item item ON item_id = item.id"
				+ " WHERE item.container_id = ? and u.login = ?";
		return unique(query.toString(), STRING_CREATOR, Arrays.asList(),
				new Object[] { container.id, login.toLowerCase() });
	}

	public boolean areValid(String[] usersUids) throws SQLException {
		String query = "select count(*) from t_domain_user d join t_container_item i ON i.id = d.item_id WHERE i.container_id = ? and i.uid = ANY(?)";
		int count = unique(query, INTEGER_CREATOR, new ArrayList<EntityPopulator<Integer>>(0),
				new Object[] { container.id, usersUids });

		return count == usersUids.length;
	}

	public void setPassword(final Item item, final String password) throws SQLException {
		update("update t_domain_user set password = ?, password_lastchange = now() where item_id = ?", password,
				new StatementValues<String>() {

					@Override
					public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
							String value) throws SQLException {
						statement.setString(index++, value);
						statement.setLong(index++, item.id);
						return index;
					}
				});
	}

	public List<String> allUids() throws SQLException {
		String query = //
				"SELECT item.uid FROM t_domain_user u, t_container_item item " + //
						" WHERE item.id = u.item_id AND item.container_id = ? ";

		return select(query.toString(), UIDFOUND_CREATOR, Collections.<EntityPopulator<String>>emptyList(),
				new Object[] { container.id });

	}

	public String getPassword(long itemId) throws SQLException {
		return unique(
				"select password from t_domain_user innser join t_container_item item on item_id = item.id where item.container_id = ? and item.id = ? ",
				STRING_CREATOR, Arrays.asList(), new Object[] { container.id, itemId });

	}
}
