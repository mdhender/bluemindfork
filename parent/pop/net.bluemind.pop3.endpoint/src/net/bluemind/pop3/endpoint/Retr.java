/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.endpoint;

import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;

public class Retr {
	private int mailSize;
	private String mail;
	public CompletableFuture<ByteBuf> completableFuture;

	public Retr(int size, CompletableFuture<ByteBuf> msgBody) {
		this.mailSize = size;
		this.completableFuture = msgBody;
	}

	public int getMailSize() {
		return mailSize;
	}

	public void setMailSize(int mailSize) {
		this.mailSize = mailSize;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

}
