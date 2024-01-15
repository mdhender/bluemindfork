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
package net.bluemind.eas.wbxml;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WbxmlOutput {

	private static final Logger logger = LoggerFactory.getLogger(WbxmlOutput.class);
	private String streamId;

	public static interface QueueDrained {

		void drained();

	}

	/**
	 * Only one byte is written
	 * 
	 * @param b
	 * @throws IOException
	 */

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	protected String streamId() {
		return streamId != null ? streamId : "unknown stream-id";
	}

	public abstract void write(int b) throws IOException;

	public abstract void write(byte[] data) throws IOException;

	public abstract void write(byte[] data, QueueDrained drained);

	public abstract String end();

	public static final WbxmlOutput of(final OutputStream os) {
		return new WbxmlOutput() {

			@Override
			public void write(int b) throws IOException {
				os.write(b);
			}

			@Override
			public void write(byte[] data) throws IOException {
				os.write(data);
			}

			@Override
			public String end() {
				try {
					os.flush();
				} catch (IOException e) {
					logger.error("[{}]: error while flusing stream", streamId(), e);
				}
				try {
					os.close();
				} catch (IOException e) {
					logger.error("[{}]: error while closing stream", e);
				}
				return null;
			}

			@Override
			public void write(byte[] data, QueueDrained qd) {
				try {
					write(data);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
				qd.drained();
			}

		};
	}

}
