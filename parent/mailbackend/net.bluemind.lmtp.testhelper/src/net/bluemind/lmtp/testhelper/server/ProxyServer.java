/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.server;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.common.io.Files;

import net.bluemind.lmtp.impl.LmtpFiltersVerticle;
import net.bluemind.lmtp.impl.LmtpProxyVerticle;
import net.bluemind.vertx.testhelper.Deploy;

public class ProxyServer {

	private static final Set<String> deployed = new HashSet<>();
	private static final File proxyConfig = new File(System.getProperty("user.home") + "/lmtpd.debug");

	public static void start() throws IOException {
		String target = "127.0.0.1:2424\n";
		Files.write(target.getBytes(), proxyConfig);
		Deploy.verticles(false, LmtpProxyVerticle::new).thenCompose(depIds -> {
			deployed.addAll(depIds);
			return Deploy.verticles(true, LmtpFiltersVerticle::new);
		}).thenAccept(depIds -> deployed.addAll(depIds)).join();
	}

	public static void stop() {
		Deploy.afterTest(deployed);
		proxyConfig.delete();
	}

}
