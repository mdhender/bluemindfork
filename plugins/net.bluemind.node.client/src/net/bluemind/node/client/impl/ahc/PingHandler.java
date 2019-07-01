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

import net.bluemind.common.io.FileBackedOutputStream;
import com.ning.http.client.HttpResponseHeaders;

import net.bluemind.node.client.impl.PingFailed;

public class PingHandler extends DefaultAsyncHandler<Void> {

	public PingHandler() {
		super(false);
	}

	@Override
	protected Void getResult(int status, HttpResponseHeaders headers, FileBackedOutputStream body) {
		if (status != 200) {
			throw new PingFailed();
		}
		return null;
	}

}
