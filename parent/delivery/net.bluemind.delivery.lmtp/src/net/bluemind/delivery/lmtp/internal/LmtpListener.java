/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.delivery.lmtp.internal;

import java.io.IOException;

import org.subethamail.smtp.TooMuchDataException;

import io.netty.buffer.ByteBuf;

public interface LmtpListener {

	/**
	 * Called once for every RCPT TO during a SMTP exchange. Each accepted recipient
	 * will result in a separate deliver() call later.
	 *
	 * @param from      is a rfc822-compliant email address.
	 * @param recipient is a rfc822-compliant email address.
	 *
	 * @return status for the recipient
	 */
	public RecipientDeliveryStatus accept(String from, String recipient);

	/**
	 * When message data arrives, this method will be called for every recipient
	 * this listener accepted.
	 *
	 * @param from       is the envelope sender in rfc822 form
	 * @param recipient  will be an accepted recipient in rfc822 form
	 * @param byteBuffer will be the smtp data stream. The data stream is only valid
	 *                   for the duration of this call.
	 *
	 * @throws TooMuchDataException if the listener can't handle that much data. An
	 *                              error will be reported to the client.
	 * @throws IOException          if there is an IO error reading the input data.
	 */
	public void deliver(String from, String recipient, ByteBuf byteBuffer) throws IOException;

}
