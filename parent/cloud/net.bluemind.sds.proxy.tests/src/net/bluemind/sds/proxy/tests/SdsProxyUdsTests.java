/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy.tests;

import java.io.File;

import io.vertx.core.net.SocketAddress;

public class SdsProxyUdsTests extends SdsProxyTests {

	@Override
	protected SocketAddress socket() {
		File f = new File("/var/run/cyrus/socket/bm-sds");
		if (!f.canRead()) {
			System.err.println("Unable to read on " + f);
		}
		if (!f.canWrite()) {
			System.err.println("Unable to write on " + f);
		}
		return SocketAddress.domainSocketAddress("/var/run/cyrus/socket/bm-sds");
	}

}
