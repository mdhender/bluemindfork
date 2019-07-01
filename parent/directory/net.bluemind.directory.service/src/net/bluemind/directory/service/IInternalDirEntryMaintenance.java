/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.directory.service;

import java.util.Set;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.task.service.IServerTaskMonitor;

public interface IInternalDirEntryMaintenance {

	/**
	 * launch maintenance operations in check mode
	 * 
	 * @param opIdentifiers
	 * @param report
	 *            report to fill (must be not null)
	 * @param monitor
	 *            progress handler
	 */
	void check(Set<String> opIdentifiers, DiagnosticReport report, IServerTaskMonitor monitor);

	/**
	 * launch maintenance operation in repair mode
	 * 
	 * @param opIdentifiers
	 * @param report
	 *            report to fill (must be not null)
	 * @param monitor
	 *            progress handler
	 */
	void repair(Set<String> opIdentifiers, DiagnosticReport report, IServerTaskMonitor monitor);
}
