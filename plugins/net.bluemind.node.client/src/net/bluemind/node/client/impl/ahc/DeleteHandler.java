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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.common.io.FileBackedOutputStream;
import com.ning.http.client.HttpResponseHeaders;

public class DeleteHandler extends DefaultAsyncHandler<Void> {

	private static final Logger logger = LoggerFactory.getLogger(DeleteHandler.class);

	private final String path;

	protected DeleteHandler(String path) {
		super(false);
		this.path = path;
	}

	@Override
	protected Void getResult(int status, HttpResponseHeaders headers, FileBackedOutputStream body) {
		if (status != 200) {
			logger.warn("DELETE {} error: {}", path, status);
		}
		return null;
	}

}
