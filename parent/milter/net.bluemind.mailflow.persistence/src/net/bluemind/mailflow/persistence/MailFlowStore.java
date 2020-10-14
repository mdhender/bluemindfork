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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.mailflow.persistence.MailFlowRuleColumns.PersistentMailflowRule;

public class MailFlowStore extends JdbcAbstractStore {

	private final String domainUid;

	public MailFlowStore(DataSource dataSource, String domainUid) {
		super(dataSource);
		this.domainUid = domainUid;
	}

	public void create(String uid, MailRuleActionAssignmentDescriptor assignment) {
		doOrFail(() -> {
			String query = "INSERT INTO t_mailflow_assignment (" + MailFlowAssignmentColumns.cols.names()
					+ ", uid, domain_uid) VALUES (" + MailFlowAssignmentColumns.cols.values() + ", ?, ? )";

			insert(query, assignment, MailFlowAssignmentColumns.statementValue(uid, this.domainUid));
			AtomicInteger idSequence = new AtomicInteger(0);
			create(uid, idSequence, 0, assignment.rules);
			return null;
		});
	}

	private void create(String uid, AtomicInteger idSequence, int parent_id, MailflowRule rule) throws SQLException {
		String query = "INSERT INTO t_mailflow_rule (" + MailFlowRuleColumns.cols.names()
				+ ", uid, domain_uid) VALUES (" + MailFlowRuleColumns.cols.values() + ", ?, ? )";

		int id = idSequence.get();
		insert(query, rule, MailFlowRuleColumns.statementValue(uid, this.domainUid, id, parent_id));

		for (MailflowRule child : rule.children) {
			idSequence.incrementAndGet();
			create(uid, idSequence, id, child);
		}
	}

	public void reCreate(String uid, MailRuleActionAssignmentDescriptor assignment) {
		doOrFail(() -> {
			delete(uid);
			create(uid, assignment);
			return null;
		});
	}

	public void delete(String uid) {
		String sql = "DELETE FROM t_mailflow_rule WHERE uid = ? and domain_uid = ?";
		String sqlAssignment = "DELETE FROM t_mailflow_assignment WHERE uid = ? and domain_uid = ?";
		doOrFail(() -> {
			delete(sql, new Object[] { uid, domainUid });
			delete(sqlAssignment, new Object[] { uid, domainUid });
			return null;
		});
	}

	public MailRuleActionAssignment get(String uid) {
		return doOrFail(() -> {

			String sql = "SELECT uid, description, position, action_identifier, execution_mode, routing, action_config, assignment_group, is_active"
					+ " from t_mailflow_assignment where domain_uid = ? and uid = ?";

			MailRuleActionAssignment assignment = unique(sql, (rs) -> new MailRuleActionAssignment(),
					MailFlowAssignmentColumns.populator(), new Object[] { this.domainUid, uid });

			if (assignment == null) {
				return null;
			}
			assignment.rules = getRules(assignment.uid);

			return assignment;
		});
	}

	public List<MailRuleActionAssignment> getAll() {

		return doOrFail(() -> {

			String sql = "SELECT uid, description, position, action_identifier, execution_mode, routing, action_config, assignment_group, is_active"
					+ " from t_mailflow_assignment where domain_uid = ?";

			List<MailRuleActionAssignment> assignments = select(sql, (rs) -> new MailRuleActionAssignment(),
					MailFlowAssignmentColumns.populator(), new Object[] { this.domainUid });

			for (MailRuleActionAssignment assignment : assignments) {
				assignment.rules = getRules(assignment.uid);
			}

			return assignments;
		});
	}

	private MailflowRule getRules(String uid) throws SQLException {
		String sql = "WITH RECURSIVE ruleconfigs(id, parent_id, rule_identifier, rule_config) AS ("
				+ "SELECT s1.id, s1.parent_id, s1.rule_identifier, s1.rule_config"
				+ " FROM t_mailflow_rule s1 WHERE s1.parent_id = 0 and s1.id = 0 and s1.uid = ? and s1.domain_uid = ?"
				+ " UNION" + " SELECT s2.id, s2.parent_id, s2.rule_identifier, s2.rule_config"
				+ " FROM t_mailflow_rule s2, ruleconfigs s1 WHERE s2.parent_id = s1.id and s2.uid = ? and s2.domain_uid = ?"
				+ ")" + " SELECT id, parent_id, rule_identifier, rule_config FROM ruleconfigs order by parent_id, id";

		List<PersistentMailflowRule> rules = select(sql, (rs) -> new PersistentMailflowRule(),
				MailFlowRuleColumns.populator(), new Object[] { uid, this.domainUid, uid, this.domainUid });
		return consolidateRules(rules);
	}

	private MailflowRule consolidateRules(List<PersistentMailflowRule> rules) {
		if (rules.isEmpty()) {
			return null;
		}
		PersistentMailflowRule rootRule = rules.get(0);
		MailflowRule root = rootRule.toMailflowRule();
		Map<Integer, MailflowRule> map = new HashMap<>();
		map.put(rootRule.id, root);

		for (int i = 1; i < rules.size(); i++) {
			PersistentMailflowRule persistentRule = rules.get(i);
			MailflowRule rule = persistentRule.toMailflowRule();
			MailflowRule parent = map.get(persistentRule.parentId);
			parent.children.add(rule);
			map.put(persistentRule.id, rule);
		}

		return root;
	}

}
