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
package net.bluemind.filehosting.service.export;

import java.io.IOException;

import org.vertx.java.core.streams.ReadStream;

public class SizeLimitedReadStream extends net.bluemind.core.rest.utils.ReadInputStream {

	private final long maxDataSize;
	private int bytesRead = 0;

	public SizeLimitedReadStream(ReadStream<?> inputStream) {
		this(inputStream, -1);
	}

	public SizeLimitedReadStream(ReadStream<?> inputStream, long maxDataSize) {
		super(inputStream);
		this.maxDataSize = maxDataSize;
	}

	@Override
	protected void beforeRead(int bytesToRead) throws IOException {
		if (maxDataSize > 0) {
			bytesRead += bytesToRead;
			if (bytesRead > maxDataSize) {
				FileSizeExceededException e = new FileSizeExceededException(maxDataSize);
				this.exception = e;
				throw e;
			}
		}

	}

}
