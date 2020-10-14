/* BEGIN LICENSE
	* Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.launcher.tests;

import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Version;

import io.airlift.airline.ParseArgumentsUnexpectedException;
import io.airlift.airline.ParseCommandUnrecognizedException;

import net.bluemind.cli.launcher.CLIManager;


public class ExitCodeCliTests {
	private static CLIManager cli;

	@BeforeClass
	public static void beforeClass() throws Exception {
		cli = new CLIManager(Version.emptyVersion);
	}

	@Test
	public void testUnrecognizedCommandArguments() {
		try {
			cli.processArgs("invalid", "command");
			fail("should have raised ParseCommandUnrecognizedException.");
		} catch (ParseArgumentsUnexpectedException | ParseCommandUnrecognizedException e) {}
	}

	@Test
	public void testException() {
		try {
			cli.processArgs("job", "status", "bm.tralala", "--job", "42");
			fail("should have failed with connection refused.");
		} catch (ParseArgumentsUnexpectedException | ParseCommandUnrecognizedException e) {
			fail("should have raised another exception (ServerFault ?)");
		} catch (Exception e) {}
	}
}
