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
package net.bluemind.cli.metrics;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.metrics.alerts.api.CheckResult;

public class Checks {
	private Checks() {
	}

	public static void printResult(CliContext ctx, String check, CheckResult result) {
		switch (result.level) {
		case OK:
			ctx.info(Strings.padEnd(check, 25, ' ') + ctx.ansi().fgBrightGreen().a(result.level.name()).reset());
			break;
		case WARN:
			ctx.warn(Strings.padEnd(check, 25, ' ') + result.level.name());
			ctx.info(Strings.nullToEmpty(result.message));
			break;
		case CRIT:
			ctx.error(Strings.padEnd(check, 25, ' ') + result.level.name());
			ctx.info(Strings.nullToEmpty(result.message));
			break;
		case UNKNOWN:
		default:
			break;

		}
	}
}
