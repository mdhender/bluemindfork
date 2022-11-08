/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.metrics.tests;

import org.junit.Test;
import org.osgi.framework.Version;

import net.bluemind.cli.launcher.CLIManager;

/**
 * Not enabled as an eclipse plugin test would need a full bluemind setup
 */
public class TickStatusTests {

	@Test
	public void testTickStatusCommand() {
		CLIManager cli = new CLIManager(new Version(4, 1, 0));
		cli.processArgs("tick", "status");
	}

}
