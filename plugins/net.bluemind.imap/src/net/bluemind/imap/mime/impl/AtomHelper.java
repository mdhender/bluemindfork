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
package net.bluemind.imap.mime.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.utils.FileUtils;

public final class AtomHelper {

	private static final Logger logger = LoggerFactory.getLogger(AtomHelper.class);

	private static final byte[] closingBraquet = "}".getBytes();

	public static final byte[] getFullResponse(String resp, InputStream followUp) {
		String orig = resp;
		byte[] envelData = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		try {
			out.write(orig.getBytes());
			if (followUp != null) {
				out.write(closingBraquet);
				FileUtils.transfer(followUp, out, true);
			}
		} catch (IOException e) {
			logger.error("error loading stream part of answer", e);
		}
		envelData = out.toByteArray();
		return envelData;
	}

}
