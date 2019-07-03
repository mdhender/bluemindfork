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
package net.bluemind.imap.impl;

import net.bluemind.imap.IMAPByteSource;

public class IMAPResponse {

	private String status;
	private boolean clientDataExpected;
	private String payload;
	private String tag;
	private IMAPByteSource streamData;

	public IMAPResponse() {

	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isOk() {
		return "OK".equals(status);
	}

	public boolean isNo() {
		return "NO".equals(status);
	}

	public boolean isBad() {
		return "BAD".equals(status);
	}

	public boolean isClientDataExpected() {
		return clientDataExpected;
	}

	public boolean isContinuation() {
		return "+".equals(tag);
	}

	public void setClientDataExpected(boolean clientDataExpected) {
		this.clientDataExpected = clientDataExpected;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public IMAPByteSource getStreamData() {
		return streamData;
	}

	public void setStreamData(IMAPByteSource fbos) {
		this.streamData = fbos;
	}
}
