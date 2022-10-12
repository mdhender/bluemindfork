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
package net.bluemind.delivery.lmtp.filters;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.delivery.lmtp.common.LmtpEnvelope;

/**
 * Allows modifying message before delivery & taking custom delivery decisions.
 * 
 * 
 */
public interface IMessageFilter {

	/**
	 * Filter messages. The message can be altered, and delivery decisions can me
	 * made using the env parameter.
	 * 
	 * 
	 * 
	 * @param env
	 * @param message the message to filter
	 * @return a filtered message. Might be the same as the given one. Return null
	 *         if you want to leave the message untouched.
	 * @throws FilterException
	 */
	public Message filter(LmtpEnvelope env, Message message) throws FilterException;
}
