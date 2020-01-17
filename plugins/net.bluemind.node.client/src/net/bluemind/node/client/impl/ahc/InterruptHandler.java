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

import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.node.shared.ExecDescriptor;

public class InterruptHandler extends DefaultAsyncHandler<Void> {

	private final ExecDescriptor desc;
	private static final Logger logger = LoggerFactory.getLogger(InterruptHandler.class);

	public InterruptHandler(ExecDescriptor req) {
		super(false);
		this.desc = req;
	}

	@Override
	protected Void getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		if (status != 200) {
			logger.warn("Interrupt of {} failed: {}", desc, status);
		}
		return null;
	}

}
