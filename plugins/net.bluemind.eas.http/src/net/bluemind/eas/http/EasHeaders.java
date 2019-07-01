/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.http;

import org.vertx.java.core.http.HttpHeaders;

public class EasHeaders {

	public static class Client {
		public static final String POLICY_KEY = "X-MS-PolicyKey";
		public static final String PROTOCOL_VERSION = "MS-ASProtocolVersion";
		public static final String ACCEPT_MULTIPART = "MS-ASAcceptMultiPart";
	}

	public static class Server {
		public static final CharSequence PROTOCOL_VERSIONS = HttpHeaders.createOptimized("MS-ASProtocolVersions");
		public static final CharSequence MS_SERVER = HttpHeaders.createOptimized("MS-Server-ActiveSync");
		public static final CharSequence SUPPORTED_COMMANDS = HttpHeaders.createOptimized("MS-ASProtocolCommands");
	}

}
