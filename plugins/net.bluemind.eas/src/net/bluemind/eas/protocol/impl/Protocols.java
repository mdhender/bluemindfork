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
package net.bluemind.eas.protocol.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.utils.RunnableExtensionLoader;

public class Protocols {

	private static final Logger logger = LoggerFactory.getLogger(Protocols.class);
	private static List<IEasProtocol<?, ?>> protocols;

	public static void registerProtocols() {
		RunnableExtensionLoader<IEasProtocol<?, ?>> rel = new RunnableExtensionLoader<>();
		protocols = rel.loadExtensions("net.bluemind.eas", "protocol", "protocol", "impl");
		logger.info("Found {} EAS protocols", protocols.size());
	}

	public static List<IEasProtocol<?, ?>> get() {
		return protocols;
	}

}
