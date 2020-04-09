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
package net.bluemind.calendar.helper.ical4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.CharBuffer;

public class IcsReader extends InputStreamReader {

	private final Writer out;

	public IcsReader(InputStream in, Writer out) {
		super(in);
		this.out = out;
	}

	@Override
	public int read() throws IOException {
		int in = super.read();
		if (in != -1) {
			out.write(in);
		}
		return in;
	}

	@Override
	public int read(char[] cbuf, int offset, int length) throws IOException {
		int read = super.read(cbuf, offset, length);
		if (read != -1) {
			out.write(cbuf, offset, read);
		}
		return read;
	}

	@Override
	public int read(CharBuffer target) throws IOException {
		int read = super.read(target);
		if (read != -1) {
			out.write(target.array(), 0, read);
		}
		return read;
	}

	@Override
	public int read(char[] cbuf) throws IOException {
		int read = super.read(cbuf);
		if (read != -1) {
			out.write(cbuf, 0, read);
		}
		return read;
	}

}
