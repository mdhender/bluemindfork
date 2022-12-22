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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Splitter;

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.imap.docker.imaptest.DovecotImaptestRunner;
import net.bluemind.imap.docker.imaptest.ImaptestPlanBuilder;

public class FullPlanTests extends MailApiTestsBase {

	/**
	 * This will fail as long as our protocol compliance is not complete.
	 */
	@Test
	public void testFullPlan() {
		DovecotImaptestRunner docker = ImaptestPlanBuilder.create()//
				.duration(Duration.ofSeconds(10))//
				.logout(1)//
				.clients(10)//
				.user(userUid + "@" + domUid, userUid).build();
		List<String> output = docker.runPlan();
		assertNotNull(output);
		assertFalse(output.isEmpty());
		Splitter lineSplit = Splitter.on('\n').omitEmptyStrings();
		List<String> justErrors = output.stream().flatMap(lineSplit::splitToStream).filter(s -> s.startsWith("Error: "))
				.toList();
		System.err.println("justErrors.size: " + justErrors.size());
		justErrors.forEach(s -> System.err.println(" - " + s));
		assertEquals("We expected 0 errors from imaptest run", 0, justErrors.size());
	}

}
