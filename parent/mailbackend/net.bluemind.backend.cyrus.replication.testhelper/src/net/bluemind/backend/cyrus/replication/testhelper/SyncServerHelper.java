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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.testhelper;

import java.util.concurrent.TimeUnit;

import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.system.stateobserver.testhelper.StateTestHelper;

public class SyncServerHelper {

	private SyncServerHelper() {
	}

	public static void waitFor() {
		StateTestHelper.blockUntilRunning();
		new NetworkHelper("127.0.0.1").waitForListeningPort(2501, 10, TimeUnit.SECONDS);
	}

}
