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
package net.bluemind.lmtp.parser;

import org.vertx.java.core.buffer.Buffer;

/**
 * lmtp request handler
 *
 */
public interface LmtpRequestHandler {

	public void handleUnknow(String cmd, String params);

	public void handleLHLO(String params);

	public void handleMAIL(String params);

	public void handleRSET(String params);

	public void handleRCPT(String params);

	public void handleNOOP();

	public void handleQUIT();

	public void handleVRFY(String params);

	public void handleDATA(String params);

	public void handleDataBuffer(Buffer buffer);

}
