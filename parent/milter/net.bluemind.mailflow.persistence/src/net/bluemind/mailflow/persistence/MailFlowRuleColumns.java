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
package net.bluemind.mailflow.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.mailflow.api.MailflowRule;

public class MailFlowRuleColumns {

	public static final Columns cols = Columns.create() //
			.col("id") //
			.col("parent_id") //
			.col("rule_identifier") //
			.col("rule_config");

	public static StatementValues<MailflowRule> statementValue(String uid, String domainUId, int id, int parentId) {
		return new StatementValues<MailflowRule>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailflowRule value) throws SQLException {

				statement.setInt(index++, id);
				statement.setInt(index++, parentId);
				statement.setString(index++, value.ruleIdentifier);
				statement.setObject(index++, value.configuration);
				statement.setString(index++, uid);
				statement.setString(index++, domainUId);
				return index;
			}
		};
	}

	public static EntityPopulator<PersistentMailflowRule> populator() {
		return new EntityPopulator<PersistentMailflowRule>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, PersistentMailflowRule value) throws SQLException {
				value.id = rs.getInt(index++);
				value.parentId = rs.getInt(index++);
				value.ruleIdentifier = rs.getString(index++);
				value.configuration = (Map<String, String>) rs.getObject(index++);
				return index;
			}
		};
	}

	public static class PersistentMailflowRule {
		public int id;
		public int parentId;
		public String ruleIdentifier;
		public Map<String, String> configuration;

		public MailflowRule toMailflowRule() {
			MailflowRule rule = new MailflowRule();
			rule.children = new ArrayList<>();
			rule.configuration = this.configuration;
			rule.ruleIdentifier = this.ruleIdentifier;
			return rule;
		}
	}

}
