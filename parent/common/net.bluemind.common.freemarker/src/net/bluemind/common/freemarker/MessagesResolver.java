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
package net.bluemind.common.freemarker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesResolver {
	private static final Logger logger = LoggerFactory.getLogger(MessagesResolver.class);
	private List<ResourceBundle> resources;

	public MessagesResolver(ResourceBundle... resources) {
		this.resources = new ArrayList<>();
		this.resources.addAll(Arrays.asList(resources));
	}

	public String translate(String messageId, Object[] params) {
		String msg = null;
		for (ResourceBundle rb : resources) {
			try {
				msg = rb.getString(messageId);
				break;
			} catch (java.util.MissingResourceException e) {
				logger.debug("error loading {}", messageId, e);
			}
		}
		if (msg == null) {
			logger.error("didnt not found value for key {}", messageId);
			return "";
		}

		logger.debug("{}:{} params {}", messageId, msg, params);
		return MessageFormat.format(msg, params);
	}

}
