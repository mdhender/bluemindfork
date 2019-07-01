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
package net.bluemind.eas.protocol;

import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.impl.Responder;

public interface IEasProtocol<Q, R> {

	void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past, Handler<Q> parserResultHandler);

	void execute(BackendSession bs, Q query, Handler<R> responseHandler);

	void write(BackendSession bs, Responder responder, R response, Handler<Void> completion);

	String address();

}
