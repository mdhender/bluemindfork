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
package net.bluemind.imap.serviceprovider;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class SPResolver {

	private static final IServiceProviderResolver INST = load();

	public static IServiceProviderResolver get() {
		return INST;
	}

	private static IServiceProviderResolver load() {
		RunnableExtensionLoader<IServiceProviderResolver> rel = new RunnableExtensionLoader<>();
		List<IServiceProviderResolver> impl = rel.loadExtensions("net.bluemind.imap.serviceprovider", "resolver", "sp",
				"impl");
		if (impl.isEmpty()) {
			throw new ServerFault("At least one IServiceProviderResolver impl must be available");
		}
		return impl.get(0);
	}
}
