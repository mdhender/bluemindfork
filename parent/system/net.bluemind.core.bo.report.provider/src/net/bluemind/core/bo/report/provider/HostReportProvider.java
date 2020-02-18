/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.bo.report.provider;

import java.util.List;
import java.util.Optional;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class HostReportProvider {
	public static Optional<IHostReport> getHostReportService() {
		RunnableExtensionLoader<IHostReport> provider = new RunnableExtensionLoader<>();
		List<IHostReport> services = provider.loadExtensions("net.bluemind.core.bo.report.provider",
				"hostreportprovider", "hostreport_provider", "impl");
		return services.stream().findAny();
	}

}
