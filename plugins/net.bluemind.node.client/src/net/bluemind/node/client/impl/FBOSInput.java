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
package net.bluemind.node.client.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.bluemind.common.io.FileBackedOutputStream;

public final class FBOSInput {

	public static final InputStream from(final FileBackedOutputStream fbos) throws IOException {
		InputStream in = fbos.asByteSource().openStream();
		FilterInputStream fin = new FilterInputStream(in) {

			@Override
			public void close() throws IOException {
				super.close();
				fbos.reset();
			}

		};
		return fin;
	}
}
