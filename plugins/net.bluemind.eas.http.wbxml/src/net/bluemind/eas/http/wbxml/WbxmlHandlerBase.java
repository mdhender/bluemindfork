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
package net.bluemind.eas.http.wbxml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.http.wbxml.internal.StreamConsumer;
import net.bluemind.eas.http.wbxml.internal.WbxmlRequestComplete;

/**
 * Abstract handler to help in the implementation of {@link IEasRequestEndpoint}
 *
 */
public abstract class WbxmlHandlerBase implements Handler<AuthorizedDeviceQuery> {

	private static final Logger logger = LoggerFactory.getLogger(WbxmlHandlerBase.class);

	@Override
	public final void handle(AuthorizedDeviceQuery event) {
		HttpServerRequest httpReq = event.request();
		StreamConsumer consumer = new StreamConsumer(event);
		if (logger.isDebugEnabled()) {
			MultiMap headers = event.request().headers();
			for (String s : headers.names()) {
				logger.debug("{}: {}", s, headers.get(s));
			}
		}
		WbxmlRequestComplete rc = new WbxmlRequestComplete(this, consumer, event);
		httpReq.handler(consumer);
		httpReq.endHandler(rc);
	}

	/**
	 * The body of the request has been read when this is called. The parsed wbxml
	 * tree is available.
	 * 
	 * Nothing has been sent to the client yet.
	 * 
	 */
	public abstract void handle(AuthorizedDeviceQuery dq, Document parsedWbxml);

}
