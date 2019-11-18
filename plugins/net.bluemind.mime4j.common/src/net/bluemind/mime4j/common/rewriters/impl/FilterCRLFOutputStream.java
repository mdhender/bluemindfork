/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.mime4j.common.rewriters.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FilterCRLFOutputStream extends FilterOutputStream {

	int last = 0;

	public static final int CR = 13;

	public static final int LF = 10;

	public static final byte[] CRLF = { CR, LF };

	public FilterCRLFOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(int ch) throws IOException {
		if (ch == CR) {
			out.write(CRLF);
		} else if (ch == LF) {
			if (last != CR) {
				out.write(CRLF);
			}
		} else {
			out.write(ch);
		}
		last = ch;
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		int d = off;
		len += off;
		for (int i = off; i < len; i++) {
			switch (b[i]) {
			case CR:
				out.write(b, d, i - d);
				out.write(CRLF, 0, 2);
				d = i + 1;
				break;
			case LF:
				if (last != CR) {
					out.write(b, d, i - d);
					out.write(CRLF, 0, 2);
				}
				d = i + 1;
				break;
			default:
				break;
			}
			last = b[i];
		}
		if (len - d > 0) {
			out.write(b, d, len - d);
		}
	}

}