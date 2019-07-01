/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.core.tick;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class TickConfigurators {

	public static List<TickInputConfigurator> configurators() {
		RunnableExtensionLoader<TickInputConfigurator> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.metrics.core", "tickInputConfigurator", "tick_input_configurator",
				"impl");
	}

}
