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

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import org.junit.Test;
import org.testcontainers.images.builder.Transferable;

import com.google.common.base.Splitter;

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.imap.docker.imaptest.DovecotImaptestRunner;
import net.bluemind.imap.docker.imaptest.ImaptestPlanBuilder;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ThunderbirdAggressivePlanTests extends MailApiTestsBase {

	private int userCount = 10;

	@Override
	public void before() throws Exception {
		super.before();
		for (int i = 0; i < userCount; i++) {
			PopulateHelper.addUser("test" + (i + 1), "test", domUid, Routing.internal, BasicRoles.ROLE_OUTLOOK);
		}

	}

	/**
	 * This will fail as long as our protocol compliance is not complete.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFullPlan() throws IOException {
		DovecotImaptestRunner docker = ImaptestPlanBuilder.create()//
				.duration(Duration.ofSeconds(120))//
				.clients(userCount)//
				.user(null, "test")//
				.profile(Transferable.of(res("profiles/tbird.conf"))).build();
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

	private byte[] res(String pathInBundle) throws IOException {
		try (InputStream in = ThunderbirdAggressivePlanTests.class.getClassLoader().getResourceAsStream(pathInBundle)) {
			assertNotNull(in);
			byte[] pristine = in.readAllBytes();
			return new String(pristine)//
					.replace("${userCount}", "" + userCount)//
					.replace("${domain}", domUid)//
					.getBytes();
		}
	}

}
