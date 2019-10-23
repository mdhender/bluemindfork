/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.metrics.alerts.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;

@BMApi(version = "3")
public class CheckResult {

	/**
	 * We chose our levels to match those of https://checkmk.com/features.html
	 *
	 */
	@BMApi(version = "3")
	public enum Level {
		OK, WARN, CRIT, UNKNOWN;
	}

	@Required
	public Level level;

	/**
	 * Optional message describe the problem.
	 * 
	 * will be null when {@link CheckResult#level} is OK
	 */
	public String message;

	public static CheckResult unknown() {
		CheckResult cr = new CheckResult();
		cr.level = Level.UNKNOWN;
		return cr;
	}

}
