/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
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
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.api.MailflowRule;
import net.bluemind.mailflow.service.validators.MailflowAssignmentValidator;

public class MailflowAssignmentValidatorTests {

	private MailRuleActionAssignmentDescriptor assignment;

	@Before
	public void before() {
		assignment = new MailRuleActionAssignmentDescriptor();
		assignment.actionIdentifier = "JournalingAction";
		assignment.rules = new MailflowRule();
		assignment.rules.ruleIdentifier = "rule1";
	}

	@Test
	public void test_invalidConfig() {
		String msg = "Mailflow configuration is null";
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + msg);
		} catch (Exception e) {
			assertEquals(msg, e.getMessage());
		}

		assignment.actionConfiguration = new HashMap<>();
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + msg);
		} catch (Exception e) {
			assertEquals(msg, e.getMessage());
		}
	}

	@Test
	public void test_invalidTargetEmail() {
		String configKey = "targetEmail";
		String msg = "Target email must not be null";
		assignment.actionConfiguration = new HashMap<>();
		assignment.actionConfiguration.put(configKey, null);
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + msg);
		} catch (Exception e) {
			assertEquals(msg, e.getMessage());
		}

		assignment.actionConfiguration.put(configKey, "");
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + msg);
		} catch (Exception e) {
			assertEquals(msg, e.getMessage());
		}

		assignment.actionConfiguration.put(configKey, "invalid.email");
		msg = "Target email 'invalid.email' does not match a valid email";
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + msg);
		} catch (Exception e) {
			assertEquals(msg, e.getMessage());
		}
	}

	@Test
	public void test_invalidFilteredEmails() {
		String configKey = "emailsFiltered";
		assignment.actionConfiguration = new HashMap<>();
		assignment.actionConfiguration.put("targetEmail", "target@valid.email");
		assignment.actionConfiguration.put(configKey, null);
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
		} catch (Exception e) {
			fail("Error must not occurs : " + e.getMessage());
		}

		assignment.actionConfiguration.put(configKey, "");
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
		} catch (Exception e) {
			fail("Error must not occurs : " + e.getMessage());
		}

		String msg = "Filtered email(s) '%s' don't match a valid email";
		assignment.actionConfiguration.put(configKey, "invalid.email");
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + String.format(msg, "invalid.email"));
		} catch (Exception e) {
			assertEquals(String.format(msg, "invalid.email"), e.getMessage());
		}

		assignment.actionConfiguration.put(configKey, "invalid.email;valid@email.fr");
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + String.format(msg, "invalid.email"));
		} catch (Exception e) {
			assertEquals(String.format(msg, "invalid.email"), e.getMessage());
		}

		assignment.actionConfiguration.put(configKey, "invalid.email;valid@email.fr");
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + String.format(msg, "invalid.email"));
		} catch (Exception e) {
			assertEquals(String.format(msg, "invalid.email"), e.getMessage());
		}

		assignment.actionConfiguration.put(configKey, "invalid.email;valid@email.fr;invalid2.email");
		try {
			MailflowAssignmentValidator validator = new MailflowAssignmentValidator();
			validator.create(assignment);
			fail("Error must occurs: " + String.format(msg, "invalid.email;invalid2.email"));
		} catch (Exception e) {
			assertEquals(String.format(msg, "invalid.email;invalid2.email"), e.getMessage());
		}
	}
}
