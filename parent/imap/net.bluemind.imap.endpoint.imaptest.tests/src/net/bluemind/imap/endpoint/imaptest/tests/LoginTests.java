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
package net.bluemind.imap.endpoint.imaptest.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.imap.docker.imaptest.DovecotImaptestRunner;
import net.bluemind.imap.docker.imaptest.ImaptestPlanBuilder;

public class LoginTests extends MailApiTestsBase {

	@Test
	public void testLoginLogout50ClientsFor10Seconds() {
		DovecotImaptestRunner docker = ImaptestPlanBuilder.create()//
				.duration(Duration.ofSeconds(10))//
				.clients(50)//
				.onlyLoginSelectLogout()//
				.select(0)//
				.logout(100)//
				.user(userUid + "@" + domUid, userUid).build();
		List<String> output = docker.runPlan();
		assertNotNull(output);
		assertFalse(output.isEmpty());
		output.forEach(System.err::print);
		List<String> lines = Arrays.asList(output.stream().collect(Collectors.joining()).split("\n"));
		String lastLine = lines.get(lines.size() - 1);
		var logins = Long.valueOf(lastLine.split(" ")[0]);
		var logouts = Long.valueOf(lastLine.split(" ")[1]);
		assertTrue("We must have equal or more logout than logins:" + lastLine, logouts >= logins);
	}

}
