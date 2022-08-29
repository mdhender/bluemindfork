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
package net.bluemind.mailflow.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailflow.api.ExecutionMode;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.api.MailActionDescriptor;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailRuleDescriptor;
import net.bluemind.mailflow.api.MailflowRouting;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailFlowServiceTests {

	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		PopulateHelper.initGlobalVirt();

		domainUid = "test" + System.nanoTime() + ".de";
		PopulateHelper.createTestDomain(domainUid);
	}

	@Test
	public void testGetRulesShouldResolveAllPossibleRules() {
		IMailflowRules rulesService = getService();

		List<MailRuleDescriptor> rules = rulesService.listRules();

		assertEquals(2, rules.size());

		List<String> identifiers = Arrays.asList(new String[] { "rule1", "rule2" });
		List<String> descriptions = Arrays.asList(new String[] { "rule 1", "rule 2" });

		for (MailRuleDescriptor rule : rules) {
			assertTrue(identifiers.contains(rule.ruleIdentifier));
			assertTrue(descriptions.contains(rule.description));
		}
	}

	@Test
	public void testGetActionsShouldResolveAllPossibleActions() {
		IMailflowRules rulesService = getService();

		List<MailActionDescriptor> actions = rulesService.listActions();

		assertEquals(6, actions.size());

		List<String> identifiers = Arrays.asList(
				new String[] { "JournalingAction", "AddSignatureAction", "action1", "action2", "action3", "action4" });
		List<String> descriptions = Arrays.asList(new String[] { "Add journaling action", "AddSignatureAction",
				"action 1", "action 2", "action 3", "action 4" });

		for (MailActionDescriptor action : actions) {
			assertTrue(identifiers.contains(action.actionIdentifier));
			assertTrue(descriptions.contains(action.description));
		}
	}

	@Test
	public void testCreatingARuleAssignment() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		rulesService.create(uid, assignment);

		MailRuleActionAssignmentDescriptor assignment2 = getAssignment("2");
		String uid2 = UUID.randomUUID().toString();
		rulesService.create(uid2, assignment2);

		List<MailRuleActionAssignment> listAssignments = rulesService.listAssignments();
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

	@Test
	public void testSanitizer() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		assignment.position = 500;
		rulesService.create(uid, assignment);

		MailRuleActionAssignmentDescriptor assignment2 = getAssignment("2");
		String uid2 = UUID.randomUUID().toString();
		assignment2.position = 50;
		rulesService.create(uid2, assignment2);

		List<MailRuleActionAssignment> listAssignments = rulesService.listAssignments();
		assertEquals(2, listAssignments.size());
		for (MailRuleActionAssignment a : listAssignments) {
			if (uid.equals(a.uid)) {
				assertEquals(99, a.position);
			} else {
				assertEquals(50, a.position);
			}
		}

		assignment.position = 5;
		assignment2.position = 123;

		rulesService.update(uid, assignment);
		rulesService.update(uid2, assignment2);

		listAssignments = rulesService.listAssignments();
		assertEquals(2, listAssignments.size());
		for (MailRuleActionAssignment a : listAssignments) {
			if (uid.equals(a.uid)) {
				assertEquals(5, a.position);
			} else {
				assertEquals(99, a.position);
			}
		}
	}

	@Test
	public void testSanitizeHTML() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		assignment.actionConfiguration = new HashMap<>();
		assignment.actionConfiguration.put("html", sampleHtml());
		assignment.actionIdentifier = "AddSignatureAction";

		String uid = UUID.randomUUID().toString();
		rulesService.create(uid, assignment);

		String sanitized = rulesService.getAssignment(uid).actionConfiguration.get("html");
		System.err.println(sampleHtmlCleaned());
		System.err.println(sanitized);
		assertEquals(sampleHtmlCleaned(), sanitized);
	}

	private String sampleHtmlCleaned() {
		return "<style> p { font-size: 2em; }</style><a href=\"http://test.fr\"></a>" + "<a>link</a>" + "<img src=\""
				+ sampleBase64Img() + "\"><span>some content</span>" + "<img style=\"height: 40px;\">";
	}

	private String sampleHtml() {
		return "<html><head><script language=\"Javascript\">Window.alert('coucou');</script></head><body>"
				+ "<style> p { font-size: 2em; }</style><a href=\"http://test.fr\"></a>"
				+ "<a href=\"javascript:my_function();window.print();\">link</a>" + "<img src=\"" + sampleBase64Img()
				+ "\"><span>some content</span><img style=\"height: 40px;\" src=\"images/img.png\"></body></html>";
	}

	private String sampleBase64Img() {
		return "data:image/gif;base64,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";
	}

	@Test
	public void testDeletingARuleAssignment() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		rulesService.create(uid, assignment);

		MailRuleActionAssignmentDescriptor assignment2 = getAssignment("2");
		String uid2 = UUID.randomUUID().toString();
		rulesService.create(uid2, assignment2);

		List<MailRuleActionAssignment> listAssignments = rulesService.listAssignments();
		assertEquals(2, listAssignments.size());

		rulesService.delete(uid);

		listAssignments = rulesService.listAssignments();
		assertEquals(1, listAssignments.size());
		assertEquals(uid2, listAssignments.get(0).uid);
		assertEquals(assignment2.actionConfiguration, listAssignments.get(0).actionConfiguration);
		assertEquals(assignment2.actionIdentifier, listAssignments.get(0).actionIdentifier);
		assertEquals(assignment2.description, listAssignments.get(0).description);
		assertEquals(assignment2.mode, listAssignments.get(0).mode);
		assertEquals(assignment2.position, listAssignments.get(0).position);
		validateSampleHierarchy(assignment2.rules, listAssignments.get(0).rules);

		rulesService.delete(uid2);
		listAssignments = rulesService.listAssignments();
		assertEquals(0, listAssignments.size());
	}

	@Test
	public void testUpdatingARuleAssignment() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		rulesService.create(uid, assignment);

		MailRuleActionAssignmentDescriptor assignment2 = getAssignment("2");
		rulesService.update(uid, assignment2);

		List<MailRuleActionAssignment> listAssignments = rulesService.listAssignments();
		assertEquals(1, listAssignments.size());
		assertEquals(uid, listAssignments.get(0).uid);
		assertEquals(assignment2.actionConfiguration, listAssignments.get(0).actionConfiguration);
		assertEquals(assignment2.actionIdentifier, listAssignments.get(0).actionIdentifier);
		assertEquals(assignment2.description, listAssignments.get(0).description);
		assertEquals(assignment2.mode, listAssignments.get(0).mode);
		assertEquals(assignment2.position, listAssignments.get(0).position);
		validateSampleHierarchy(assignment2.rules, listAssignments.get(0).rules);
	}

	@Test
	public void testCreatingARuleAssignmentUsingAnUnknownRuleIdentifierShouldFail() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		try {
			assignment.rules.children.get(0).ruleIdentifier = "iWillFail";
			rulesService.create(uid, assignment);
			fail();
		} catch (ServerFault e) {
		}

	}

	@Test
	public void testCreatingARuleUsingAnUnknownActionIdentifierShouldFail() {
		IMailflowRules rulesService = getService();

		MailRuleActionAssignmentDescriptor assignment = getAssignment("1");
		String uid = UUID.randomUUID().toString();
		try {
			assignment.actionIdentifier = "iWillFail";
			rulesService.create(uid, assignment);
			fail();
		} catch (ServerFault e) {
		}

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
		assignment.routing = MailflowRouting.OUTGOING;
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

	private IMailflowRules getService() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailflowRules.class, domainUid);
	}

}
