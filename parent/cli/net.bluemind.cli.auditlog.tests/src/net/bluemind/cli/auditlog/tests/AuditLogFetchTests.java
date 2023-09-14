/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.cli.auditlog.tests;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.cli.auditlog.tests.utils.CliTestHelper;
import net.bluemind.cli.auditlog.tests.utils.CliTestHelper.TestDomainOptions;
import net.bluemind.cli.launcher.CLIManager;

public class AuditLogFetchTests {

	private static String domainUid;
	private static CliTestHelper testHelper;
	private static CLIManager cli;
	public static final int USER_COUNT = 10;

	@BeforeClass
	public static void beforeClass() throws Exception {
		domainUid = "junit" + System.currentTimeMillis() + ".cli";
		testHelper = CliTestHelper.builder()//
				.withDomains(domainUid)//
				.enableCyrusReplication()//
				.withDomainOptions(TestDomainOptions.justUsers(USER_COUNT))//
				.build();
		testHelper.beforeTest();
		cli = new CLIManager(Activator.context.getBundle().getVersion());
	}

	@Test
	public void testGetAuditLogs() {
		cli.processArgs("auditlog", "fetch", "--logtype", "calendar");
//		cli.processArgs("user", "get", domainUid);
		String output = testHelper.outputAndReset();
//		assertNotNull(output);
//		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
//				.map(l -> new JsonObject(l)).collect(Collectors.toList());
//		assertFalse(entryObjects.isEmpty());
//		System.out.println("Found entries: " + entryObjects.size());
//		assertTrue("We provisionned " + USER_COUNT + " users but got " + entryObjects.size(),
//				entryObjects.size() >= USER_COUNT);
		assertTrue(false);
	}
}
