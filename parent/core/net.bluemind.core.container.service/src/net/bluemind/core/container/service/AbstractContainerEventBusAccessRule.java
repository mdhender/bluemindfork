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
package net.bluemind.core.container.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IEventBusAccessRule;

public class AbstractContainerEventBusAccessRule implements IEventBusAccessRule {

	private String baseAddress;
	private static final Logger logger = LoggerFactory.getLogger(AbstractContainerEventBusAccessRule.class);

	public AbstractContainerEventBusAccessRule(String baseAddress) {
		this.baseAddress = baseAddress;
	}

	@Override
	public boolean match(String path) {
		if (path.startsWith(baseAddress) && path.endsWith(".changed")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean authorize(BmContext context, String path) {
		String uid = path.substring(baseAddress.length() + 1);
		uid = uid.substring(0, uid.length() - ".changed".length());
		try {
			return new RBACManager(context).forContainer(uid).can(Verb.Read.name());
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.NOT_FOUND) {
				logger.info("Authorization on non-existing container {} requested", uid);
				return false;
			} else {
				throw e;
			}
		}
	}

}
