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

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Version;

import net.bluemind.cli.launcher.CLIManager;

public class ExitCodeCliTests {
	private static CLIManager cli;

	@BeforeClass
	public static void beforeClass() throws Exception {
		cli = new CLIManager(Version.emptyVersion);
	}

	@Test
	public void testUnrecognizedCommandArguments() {
		assertEquals(51, cli.processArgs("invalid", "command"));
	}

	@Test
	public void testException() {
		assertEquals(1, cli.processArgs("job", "status", "bm.tralala", "--job", "42"));
	}
}
