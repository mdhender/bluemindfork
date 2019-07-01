/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.service.internal;

import java.util.List;

import net.bluemind.monitoring.service.IServiceInfoProvider;

public class ServiceInfoProvider {

	public final IServiceInfoProvider impl;
	public final String serviceName;
	public final List<String> tags;

	public ServiceInfoProvider(IServiceInfoProvider impl, String serviceName, List<String> tags) {
		this.impl = impl;
		this.serviceName = serviceName;
		this.tags = tags;
	}

}
