/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.milter.impl;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.Files;

import io.vertx.core.buffer.Buffer;

public class TrafficRecorder implements ITrafficRecorder {

	private int packet;
	private final File root;
	private static final Logger logger = LoggerFactory.getLogger(TrafficRecorder.class);

	public TrafficRecorder(String sid) {

		this.packet = 0;
		this.root = new File(System.getProperty("user.home") + "/miltered/" + sid.replace("__vertx.net.", ""));
		root.mkdirs();
	}

	public void record(Buffer buf) {
		String fn = "packet." + Strings.padStart(Integer.toString(packet++), 10, '0') + ".buf";
		try {
			Files.write(buf.getBytes(), new File(root, fn));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
