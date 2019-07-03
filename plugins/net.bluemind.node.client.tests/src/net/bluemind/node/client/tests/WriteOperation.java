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
package net.bluemind.node.client.tests;

import java.io.ByteArrayInputStream;

import net.bluemind.node.api.INodeClientFactory;

public class WriteOperation implements Runnable {

	private final byte[] toWrite;
	private final String path;
	private final INodeClientFactory facto;
	private final StatRecorder sr;

	public WriteOperation(INodeClientFactory facto, StatRecorder sr, String path, byte[] toWrite) {
		this.toWrite = toWrite;
		this.path = path;
		this.facto = facto;
		this.sr = sr;
	}

	@Override
	public void run() {
		try {
			facto.create("127.0.0.1").writeFile(path, new ByteArrayInputStream(toWrite));
			sr.ok();
		} catch (Exception t) {
			sr.ko(t);
		}
	}

}
