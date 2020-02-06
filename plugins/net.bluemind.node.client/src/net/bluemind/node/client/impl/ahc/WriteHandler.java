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
package net.bluemind.node.client.impl.ahc;

import java.io.InputStream;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.request.body.generator.InputStreamBodyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;

public class WriteHandler extends DefaultAsyncHandler<Void> {

	private static final Logger logger = LoggerFactory.getLogger(WriteHandler.class);

	private final InputStream source;
	private final String path;

	public WriteHandler(String path, InputStream source) {
		super(false);
		this.path = path;
		this.source = source;
	}

	@Override
	protected Void getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		if (status != 200) {
			logger.warn("PUT {} error: {}", path, status);
			throw new ServerFault();
		}
		return null;
	}

	@Override
	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		rb.setBody(new InputStreamBodyGenerator(source));
		return rb;
	}

}
