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

import org.asynchttpclient.HttpResponseStatus;

import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.node.client.DoesNotExist;

public class ReadHandler extends DefaultAsyncHandler<FileBackedOutputStream> {

	public ReadHandler() {
		super(true);
	}

	@Override
	protected FileBackedOutputStream getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		return body;
	}

	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		if (responseStatus.getStatusCode() == 404) {
			throw new DoesNotExist();
		} else {
			return super.onStatusReceived(responseStatus);
		}
	}

}
