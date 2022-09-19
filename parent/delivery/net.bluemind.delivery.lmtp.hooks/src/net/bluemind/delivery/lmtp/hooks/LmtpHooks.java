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
package net.bluemind.delivery.lmtp.hooks;

import java.util.List;

import net.bluemind.delivery.lmtp.common.IDeliveryHook;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class LmtpHooks {

	private LmtpHooks() {

	}

	private static final List<IDeliveryHook> PLUGINS = load();

	private static List<IDeliveryHook> load() {
		RunnableExtensionLoader<IDeliveryHook> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensionsWithPriority("net.bluemind.delivery.lmtp.hooks", "factory", "factory", "impl");

	}

	public static List<IDeliveryHook> get() {
		return PLUGINS;
	}

}
