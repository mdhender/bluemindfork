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
package net.bluemind.imap;

public class Summary {

	private final int uid;
	private IMAPHeaders headers;
	private FlagsList flags;
	private InternalDate date;
	private int size;

	public Summary(int uid) {
		this.uid = uid;
	}

	public int getUid() {
		return uid;
	}

	public IMAPHeaders getHeaders() {
		return headers;
	}

	public void setHeaders(IMAPHeaders headers) {
		this.headers = headers;
	}

	public FlagsList getFlags() {
		return flags;
	}

	public void setFlags(FlagsList flags) {
		this.flags = flags;
	}

	public InternalDate getDate() {
		return date;
	}

	public void setDate(InternalDate date) {
		this.date = date;
	}

	/**
	 * @return RFC822.SIZE (in Bytes)
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Sets the message size
	 * 
	 * @param size
	 *            in Bytes
	 */
	public void setSize(int size) {
		this.size = size;
	}

}
