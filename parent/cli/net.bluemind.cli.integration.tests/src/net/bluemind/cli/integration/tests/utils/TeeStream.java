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
package net.bluemind.cli.integration.tests.utils;

import java.io.PrintStream;

public class TeeStream extends PrintStream {
	PrintStream outStream;

	public TeeStream(PrintStream out1, PrintStream out2) {
		super(out1);
		this.outStream = out2;
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		try {
			super.write(buf, off, len);
			outStream.write(buf, off, len);
		} catch (Exception e) {
			setError();
		}
	}

	@Override
	public void flush() {
		super.flush();
		outStream.flush();
	}

}