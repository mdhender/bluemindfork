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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.sendmail.tests;

import java.io.IOException;
import java.io.InputStream;

import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;

public class TestMessageListener implements SimpleMessageListener {

	private long total;

	@Override
	public boolean accept(String from, String recipient) {
		System.err.println("from: " + from + ", recipient: " + recipient);
		return true;
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
		CountingOutputStream out = new CountingOutputStream(ByteStreams.nullOutputStream());
		ByteStreams.copy(data, out);
		this.total += out.getCount();
	}

	public long total() {
		return total;
	}

}
