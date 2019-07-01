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
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import com.google.common.io.ByteSource;

import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.wbxml.internal.BlobRequestComplete;
import net.bluemind.eas.http.wbxml.internal.StreamConsumer;

/**
 * Use it as a base for large incoming request, like a new mail with attachments
 *
 */
public abstract class BlobHandlerBase implements Handler<AuthorizedDeviceQuery> {

	private static final Logger logger = LoggerFactory.getLogger(BlobHandlerBase.class);

	protected BlobHandlerBase() {
	}

	@Override
	public final void handle(AuthorizedDeviceQuery query) {
		try {
			HttpServerRequest req = query.request();
			StreamConsumer sc = new StreamConsumer(query);
			BlobRequestComplete brc = new BlobRequestComplete(this, sc, query);
			req.dataHandler(sc);
			req.endHandler(brc);
		} catch (Exception e) {
			logger.error("Error while sending mail.", e);
		}
	}

	/**
	 * The body of the request has been read when this is called. The parsed
	 * wbxml tree is available.
	 * 
	 * Nothing has been sent to the client yet.
	 * 
	 */
	public abstract void handle(AuthorizedDeviceQuery dq, ByteSource incomingBlob, Handler<Void> completion);

}
