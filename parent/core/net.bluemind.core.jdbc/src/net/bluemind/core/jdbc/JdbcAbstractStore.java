/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.jdbc;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;

public class JdbcAbstractStore {

	protected static final Logger logger = LoggerFactory.getLogger(JdbcAbstractStore.class);
	private static final String BYTEA = "bytea";

	protected final DataSource datasource;

	public JdbcAbstractStore(DataSource dataSource) {
		this.datasource = dataSource;
	}

	protected <T> List<T> select(String query, Creator<T> creator, EntityPopulator<T> populator) throws SQLException {
		return select(query, creator, Arrays.asList(populator), null);
	}

	protected <T> List<T> select(String query, Creator<T> creator, EntityPopulator<T> populator, Object[] parameters)
			throws SQLException {
		if (populator != null) {
			return select(query, creator, Arrays.asList(populator), parameters);
		} else {
			return select(query, creator, Collections.emptyList(), parameters);
		}
	}

	protected <T> List<T> select(String query, Creator<T> creator, List<EntityPopulator<T>> populators,
			Object[] parameters) throws SQLException {
		Connection conn = getConnection();
		List<T> ret = new ArrayList<>();
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					Object param = parameters[i];
					if (param instanceof Long[]) {
						st.setArray(i + 1, conn.createArrayOf("int", (Long[]) param));
					} else if (param instanceof String[]) {
						st.setArray(i + 1, conn.createArrayOf("text", (String[]) param));
					} else if (param instanceof Byte[]) {
						st.setArray(i + 1, conn.createArrayOf(BYTEA, (Byte[]) param));
					} else {
						st.setObject(i + 1, param);
					}
				}
			}
			logger.debug("[{}] S: {}", datasource, st);
			long time = System.currentTimeMillis();
			rs = st.executeQuery();
			long elapsedTime = System.currentTimeMillis() - time;
			if (elapsedTime > 300) {
				logger.warn("S: {} took {}ms", st, elapsedTime);
			} else {
				logger.trace("S: {} took {}ms", st, elapsedTime);
			}
			int count = 0;
			while (rs.next()) {
				int index = 1;
				T v = creator.create(rs);
				for (EntityPopulator<T> populator : populators) {
					index = populator.populate(rs, index, v);
				}

				ret.add(v);
				count++;
				logger.debug("   Found one: {}", v);
			}
			logger.debug("Total found: {}", count);
		} finally {
			JdbcHelper.cleanup(conn, rs, st);
		}
		return ret;
	}

	protected <T> T unique(String query, Creator<T> creator, EntityPopulator<T> populator, Object param)
			throws SQLException {
		Connection conn = getConnection();
		ResultSet rs = null;
		T v = null;
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);
			if (param instanceof Long[]) {
				st.setArray(1, conn.createArrayOf("int", (Long[]) param));
			} else if (param instanceof String[]) {
				st.setArray(1, conn.createArrayOf("text", (String[]) param));
			} else if (param instanceof Byte[]) {
				st.setArray(1, conn.createArrayOf(BYTEA, (Byte[]) param));
			} else {
				st.setObject(1, param);
			}
			logger.debug("[{}] S: {}", datasource, st);
			long time = System.currentTimeMillis();
			rs = st.executeQuery();
			long elapsedTime = System.currentTimeMillis() - time;
			if (elapsedTime > 300) {
				logger.warn("S: {} took {}ms", st, elapsedTime);
			} else {
				logger.trace("S: {} took {}ms", st, elapsedTime);
			}
			if (rs.next()) {
				v = creator.create(rs);
				populator.populate(rs, 1, v);
			}
		} finally {
			JdbcHelper.cleanup(conn, rs, st);
		}
		return v;
	}

	protected <T> T unique(String query, Creator<T> creator, List<EntityPopulator<T>> populators) throws SQLException {
		List<T> ret = select(query, creator, populators, null);
		if (!ret.isEmpty()) {
			return ret.get(0);
		} else {
			return null;
		}
	}

	protected <T> T unique(String query, Creator<T> creator, List<EntityPopulator<T>> populators, Object[] parameters)
			throws SQLException {
		List<T> ret = select(query, creator, populators, parameters);
		if (!ret.isEmpty()) {
			return ret.get(0);
		} else {
			return null;
		}
	}

	protected <T> T unique(String query, Creator<T> creator, EntityPopulator<T> populators, Object[] parameters)
			throws SQLException {
		return unique(query, creator, Arrays.asList(populators), parameters);
	}

	protected <T> T unique(String query, Creator<T> creator, EntityPopulator<T> populator) throws SQLException {
		List<T> ret = select(query, creator, Arrays.asList(populator), null);
		if (!ret.isEmpty()) {
			return ret.get(0);
		} else {
			return null;
		}
	}

	@FunctionalInterface
	public interface StatementValues<T> {
		public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, T value)
				throws SQLException;
	}

	public enum DataType {
		TEXT, NUMERIC
	}

	@FunctionalInterface
	public interface Creator<T> {
		public T create(ResultSet con) throws SQLException;
	}

	@FunctionalInterface
	public interface EntityPopulator<T> {
		public int populate(ResultSet rs, int index, T value) throws SQLException;
	}

	protected <T> int update(String query, T value, StatementValues<T> values) throws SQLException {
		return update(query, value, values, null);
	}

	protected <T> int update(String query, T value, StatementValues<T> values, Object[] parameters)
			throws SQLException {
		return update(query, value, Arrays.asList(values), parameters);
	}

	protected int update(String query, Object[] parameters) throws SQLException {
		return update(query, null, Collections.emptyList(), parameters);
	}

	protected <T> int update(String query, T value, List<StatementValues<T>> stValues, Object[] parameters)
			throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);
			int index = 1;
			for (StatementValues<T> stValue : stValues) {
				index = stValue.setValues(conn, st, index, 0, value);
			}
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					Object param = parameters[i];
					if (param instanceof String[]) {
						st.setArray(index++, conn.createArrayOf("text", (String[]) param));
					} else if (param instanceof Long[]) {
						st.setArray(index++, conn.createArrayOf("int4", (Long[]) param));
					} else if (param instanceof Byte[]) {
						st.setArray(index++, conn.createArrayOf(BYTEA, (Byte[]) param));
					} else {
						st.setObject(index++, parameters[i]);
					}
				}
			}
			logger.debug("[{}] U: {}", datasource, st);
			return st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);

		}
	}

	protected <T> int update(String query, T value, Object[] parameters) throws SQLException {
		return update(query, value, Collections.emptyList(), parameters);
	}

	protected <T> void insert(String query, T value, StatementValues<T> values) throws SQLException {
		insert(query, value, Arrays.asList(values));
	}

	protected int delete(String query, Object[] parameters) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					Object param = parameters[i];
					if (param instanceof String[]) {
						st.setArray(i + 1, conn.createArrayOf("text", (String[]) param));
					} else if (param instanceof Long[]) {
						st.setArray(i + 1, conn.createArrayOf("int4", (Long[]) param));
					} else if (param instanceof Byte[]) {
						st.setArray(i + 1, conn.createArrayOf(BYTEA, (Byte[]) param));
					} else {
						st.setObject(i + 1, parameters[i]);
					}
				}
			}
			logger.debug("[{}] D: {}", datasource, st);
			return st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);
		}
	}

	protected <T> List<T> delete(String query, Creator<T> creator, List<EntityPopulator<T>> populators)
			throws SQLException {
		return delete(query, creator, populators, null);
	}

	protected <T> List<T> delete(String query, Creator<T> creator, List<EntityPopulator<T>> populators,
			Object[] parameters) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
		List<T> ret = new ArrayList<>();
		try {
			st = conn.prepareStatement(query);
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					Object param = parameters[i];
					if (param instanceof String[]) {
						st.setArray(i + 1, conn.createArrayOf("text", (String[]) param));
					} else if (param instanceof Long[]) {
						st.setArray(i + 1, conn.createArrayOf("int4", (Long[]) param));
					} else if (param instanceof Byte[]) {
						st.setArray(i + 1, conn.createArrayOf(BYTEA, (Byte[]) param));
					} else {
						st.setObject(i + 1, parameters[i]);
					}
				}
			}
			rs = st.executeQuery();
			while (rs.next()) {
				int index = 1;
				T v = creator.create(rs);
				for (EntityPopulator<T> populator : populators) {
					index = populator.populate(rs, index, v);
				}
				ret.add(v);
			}
		} finally {
			JdbcHelper.cleanup(conn, rs, st);
		}
		return ret;
	}

	protected <T> void batchInsert(String query, Collection<T> values, StatementValues<T> statementValues)
			throws SQLException {
		batchInsert(query, values, Arrays.asList(statementValues));
	}

	protected <T> void batchInsert(String query, Collection<T> values, Collection<StatementValues<T>> statementValues)
			throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);

			int row = 0;
			for (T v : values) {
				int index = 1;
				for (StatementValues<T> stValue : statementValues) {

					index = stValue.setValues(conn, st, index, row, v);
				}
				st.addBatch();
				row++;
			}
			logger.debug("[{}] batch I: {}", datasource, st);
			st.executeBatch();
		} catch (BatchUpdateException bue) {
			throw bue.getNextException();
		} finally {
			JdbcHelper.cleanup(conn, null, st);

		}
	}

	protected <T> void insert(String query, T value, List<StatementValues<T>> values) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);
			int index = 1;
			for (StatementValues<T> stValue : values) {
				index = stValue.setValues(conn, st, index, 0, value);
			}
			logger.debug("[{}] I: {}", datasource, st);
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);

		}
	}

	protected <T> void insert(String query, T value, StatementValues<T> stValue, Object[] parameters)
			throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(query);
			int index = 1;
			index = stValue.setValues(conn, st, index, 0, value);
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					Object param = parameters[i];
					if (param instanceof String[]) {
						st.setArray(index++, conn.createArrayOf("text", (String[]) param));
					} else if (param instanceof Long[]) {
						st.setArray(index++, conn.createArrayOf("int4", (Long[]) param));
					} else if (param instanceof Byte[]) {
						st.setArray(index++, conn.createArrayOf(BYTEA, (Byte[]) param));
					} else {
						st.setObject(index++, parameters[i]);
					}
				}
			}
			logger.debug("[{}] I: {}", datasource, st);
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);

		}

	}

	protected <T, P> P insertAndReturn(String query, T value, List<StatementValues<T>> values, Creator<P> creator,
			EntityPopulator<P> populator) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(query);
			int index = 1;
			for (StatementValues<T> stValue : values) {
				index = stValue.setValues(conn, st, index, 0, value);
			}
			logger.debug("[{}] I: {}", datasource, st);
			rs = st.executeQuery();
			if (rs.next()) {

				P v = creator.create(rs);
				if (populator != null) {
					populator.populate(rs, 1, v);
				}

				return v;
			} else {
				return null;
			}
		} finally {
			JdbcHelper.cleanup(conn, rs, st);

		}
	}

	protected int insert(String query, Object[] parameters) throws SQLException {
		try (Connection conn = getConnection()) {
			return insertImpl(query, parameters, conn);
		}
	}

	private int insertImpl(String query, Object[] parameters, Connection conn) throws SQLException {
		try (PreparedStatement st = conn.prepareStatement(query)) {
			int index = 1;
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					Object param = parameters[i];
					if (param instanceof String[]) {
						st.setArray(index++, conn.createArrayOf("text", (String[]) param));
					} else if (param instanceof Long[]) {
						st.setArray(index++, conn.createArrayOf("int4", (Long[]) param));
					} else if (param instanceof Byte[]) {
						st.setArray(index++, conn.createArrayOf(BYTEA, (Byte[]) param));
					} else {
						st.setObject(index++, parameters[i]);
					}
				}
			}
			logger.debug("[{}] I: {}", datasource, st);
			return st.executeUpdate();
		}
	}

	protected int insertWithSerial(String query, Object[] parameters) throws SQLException {
		try (Connection conn = getConnection()) {
			insertImpl(query, parameters, conn);
			return lastInsertId(conn);
		}
	}

	protected Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public int lastInsertId(Connection con) throws SQLException {
		int ret = 0;
		Statement st = null;
		ResultSet rs = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery("SELECT lastval()");
			if (rs.next()) {
				ret = rs.getInt(1);
			}
		} finally {
			JdbcHelper.cleanup(null, rs, st);
		}
		return ret;
	}

	@FunctionalInterface
	public interface SqlOperation<R> {
		R execute() throws SQLException;
	}

	public static <R> R doOrFail(SqlOperation<R> op) {
		try {
			return op.execute();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public static <R> R doOrContinue(String action, SqlOperation<R> op) {
		try {
			return op.execute();
		} catch (Exception e) {
			logger.warn("error applying {} ", action, e);
			return null;
		}
	}

}
