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
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.mailflow.api.ExecutionMode;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;

public class MailFlowAssignmentColumns {

	public static final Columns cols = Columns.create() //
			.col("description") //
			.col("position") //
			.col("action_identifier") //
			.col("execution_mode", "enum_mailflow_execution_mode") //
			.col("action_config") //
			.col("assignment_group") //
			.col("is_active");

	public static StatementValues<MailRuleActionAssignmentDescriptor> statementValue(String uid, String domainUId) {
		return new StatementValues<MailRuleActionAssignmentDescriptor>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailRuleActionAssignmentDescriptor value) throws SQLException {

				statement.setString(index++, value.description);
				statement.setInt(index++, value.position);
				statement.setString(index++, value.actionIdentifier);
				statement.setString(index++, value.mode.name());
				statement.setObject(index++, value.actionConfiguration);
				statement.setString(index++, value.group);
				statement.setBoolean(index++, value.isActive);
				statement.setString(index++, uid);
				statement.setString(index++, domainUId);
				return index;
			}
		};
	}

	public static EntityPopulator<MailRuleActionAssignment> populator() {
		return new EntityPopulator<MailRuleActionAssignment>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, MailRuleActionAssignment value) throws SQLException {

				value.uid = rs.getString(index++);
				value.description = rs.getString(index++);
				value.position = rs.getInt(index++);
				value.actionIdentifier = rs.getString(index++);
				value.mode = ExecutionMode.valueOf(rs.getString(index++));
				value.actionConfiguration = (Map<String, String>) rs.getObject(index++);
				value.group = rs.getString(index++);
				value.isActive = rs.getBoolean(index++);
				return index;
			}
		};

	}

}
