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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailflow.api.ExecutionMode;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailflowRule;

public class MailFlowStoreTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
	}

	@Test
	public void testCreatingARuleAssignment() {
		MailFlowStore rulesService = getStore();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		rulesService.create(uid, assignment);

		MailRuleActionAssignmentDescriptor assignment2 = getAssignment("2");
		String uid2 = UUID.randomUUID().toString();
		rulesService.create(uid2, assignment2);

		List<MailRuleActionAssignment> listAssignments = rulesService.getAll();
		assertEquals(2, listAssignments.size());
		MailRuleActionAssignment mailRuleActionAssignment1 = listAssignments.stream().filter(m -> {
			return m.uid.equals(uid);
		}).findFirst().get();

		assertEquals(uid, mailRuleActionAssignment1.uid);
		assertEquals(assignment.actionConfiguration, mailRuleActionAssignment1.actionConfiguration);
		assertEquals(assignment.actionIdentifier, mailRuleActionAssignment1.actionIdentifier);
		assertEquals(assignment.description, mailRuleActionAssignment1.description);
		assertEquals(assignment.mode, mailRuleActionAssignment1.mode);
		assertEquals(assignment.position, mailRuleActionAssignment1.position);
		validateSampleHierarchy(assignment.rules, mailRuleActionAssignment1.rules);

		MailRuleActionAssignment mailRuleActionAssignment2 = listAssignments.stream().filter(m -> {
			return m.uid.equals(uid2);
		}).findFirst().get();

		assertEquals(uid2, mailRuleActionAssignment2.uid);
		assertEquals(assignment2.actionConfiguration, mailRuleActionAssignment2.actionConfiguration);
		assertEquals(assignment2.actionIdentifier, mailRuleActionAssignment2.actionIdentifier);
		assertEquals(assignment2.description, mailRuleActionAssignment2.description);
		assertEquals(assignment2.mode, mailRuleActionAssignment2.mode);
		assertEquals(assignment2.position, mailRuleActionAssignment2.position);
		validateSampleHierarchy(assignment2.rules, mailRuleActionAssignment2.rules);

	}

	private MailFlowStore getStore() {
		return new MailFlowStore(JdbcTestHelper.getInstance().getDataSource(), "domain1");
	}

	@Test
	public void testDeletingARuleAssignment() {
		MailFlowStore rulesService = getStore();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		rulesService.create(uid, assignment);

		MailRuleActionAssignmentDescriptor assignment2 = getAssignment("2");
		String uid2 = UUID.randomUUID().toString();
		rulesService.create(uid2, assignment2);

		List<MailRuleActionAssignment> listAssignments = rulesService.getAll();
		assertEquals(2, listAssignments.size());

		rulesService.delete(uid);

		listAssignments = rulesService.getAll();
		assertEquals(1, listAssignments.size());
		assertEquals(uid2, listAssignments.get(0).uid);
		assertEquals(assignment2.actionConfiguration, listAssignments.get(0).actionConfiguration);
		assertEquals(assignment2.actionIdentifier, listAssignments.get(0).actionIdentifier);
		assertEquals(assignment2.description, listAssignments.get(0).description);
		assertEquals(assignment2.mode, listAssignments.get(0).mode);
		assertEquals(assignment2.position, listAssignments.get(0).position);
		validateSampleHierarchy(assignment2.rules, listAssignments.get(0).rules);

		rulesService.delete(uid2);
		listAssignments = rulesService.getAll();
		assertEquals(0, listAssignments.size());
	}

	private MailflowRule createSampleRuleHierarchy(String id) {
		MailflowRule root = new MailflowRule();
		root.configuration = new HashMap<>();
		root.configuration.put(id + "root-key1", id + "root-key1");
		root.ruleIdentifier = "rule1";
		root.children = new ArrayList<>();
		root.children.add(createRule(id, "rule1"));
		root.children.add(createRule(id, "rule2"));

		return root;
	}

	private MailflowRule createRule(String id, String ruleIdentifier) {
		MailflowRule rule = new MailflowRule();
		rule.configuration = new HashMap<>();
		rule.configuration.put(id + ruleIdentifier + "-key", id + ruleIdentifier + "-value");
		rule.ruleIdentifier = ruleIdentifier;
		return rule;
	}

	private MailRuleActionAssignmentDescriptor getAssignment(String id) {
		MailRuleActionAssignmentDescriptor assignment = new MailRuleActionAssignmentDescriptor();
		assignment.actionConfiguration = new HashMap<>();
		assignment.actionConfiguration.put(id + "action-key1", id + "action-key1");
		assignment.actionConfiguration.put(id + "action-key2", id + "action-key2");
		assignment.actionIdentifier = "action1";
		assignment.description = id + "Add a disclaimer when my rule matches";
		assignment.mode = ExecutionMode.CONTINUE;
		assignment.position = 3;
		assignment.rules = createSampleRuleHierarchy(id);
		return assignment;
	}

	private void validateSampleHierarchy(MailflowRule expected, MailflowRule actual) {
		assertEquals(expected.configuration, actual.configuration);
		assertEquals(expected.ruleIdentifier, actual.ruleIdentifier);
		assertEquals(expected.children.size(), actual.children.size());

		if (!expected.children.isEmpty()) {
			int index = 0;
			for (MailflowRule expectedChild : expected.children) {
				validateSampleHierarchy(expectedChild, actual.children.get(index++));
			}
		}
	}

}
