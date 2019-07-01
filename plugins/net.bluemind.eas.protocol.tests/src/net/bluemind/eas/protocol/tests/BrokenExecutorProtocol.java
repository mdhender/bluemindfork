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
package net.bluemind.eas.protocol.tests;

import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.protocol.tests.BrokenExecutorProtocol.BrokenReq;
import net.bluemind.eas.protocol.tests.BrokenExecutorProtocol.BrokenResp;

public class BrokenExecutorProtocol implements IEasProtocol<BrokenReq, BrokenResp> {

	public static class BrokenReq {

	}

	public static class BrokenResp {

	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<BrokenReq> parserResultHandler) {
		parserResultHandler.handle(new BrokenReq());
	}

	@Override
	public void execute(BackendSession bs, BrokenReq query, Handler<BrokenResp> responseHandler) {
		throw new NullPointerException();
	}

	@Override
	public void write(BackendSession bs, Responder responder, BrokenResp response, Handler<Void> completion) {
		completion.handle(null);
	}

	@Override
	public String address() {
		return "junit.broken.execute";
	}

}
