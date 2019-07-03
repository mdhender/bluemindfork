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

	char prev = 0;
	private boolean first = true;

	public FilterCRLFOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(int b) throws IOException {

		char asChar = (char) b;
		if (!first && asChar == '\n' && prev != '\r') {
			super.write('\r');
		}
		prev = asChar;
		first = false;
		super.write(b);
	}

}