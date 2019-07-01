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
package net.bluemind.eas.command.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;

import com.google.common.io.ByteSource;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.wbxml.BlobHandlerBase;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.compat.SessionWrapper;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;

public abstract class MailRequestBase extends BlobHandlerBase {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	protected IBackend backend;

	protected MailRequestBase() {
		this.backend = Backends.dataAccess();
	}

	public abstract void process(AuthorizedDeviceQuery dq, BackendSession bs, ByteSource mailContent,
			boolean saveInSent, Responder responder, Handler<Void> completion);

	public void handle(AuthorizedDeviceQuery dq, ByteSource incomingBlob, Handler<Void> completion) {
		boolean saveInSent = false;
		String sis = dq.optionalParams().saveInSent();
		if (sis != null) {
			saveInSent = sis.equalsIgnoreCase("T");
		}
		BackendSession bs = SessionWrapper.wrap(dq);
		Responder responder = new VertxResponder(dq.request(), dq.request().response());
		process(dq, bs, incomingBlob, saveInSent, responder, completion);
	}

}
