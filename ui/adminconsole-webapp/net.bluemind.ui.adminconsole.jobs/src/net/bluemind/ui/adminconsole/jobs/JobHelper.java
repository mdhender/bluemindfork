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
package net.bluemind.ui.adminconsole.jobs;

import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobExitStatus;

public class JobHelper {

	public static String getShortId(Job j) {
		String n = j.id;
		int idx = n.lastIndexOf(".");
		if (idx >= 0) {
			n = n.substring(idx + 1);
		}
		return n;
	}

	public static String i18n(JobExitStatus bes) {
		switch (bes) {
		case COMPLETED_WITH_WARNINGS:
			return JobTexts.INST.warningStatus();
		case FAILURE:
			return JobTexts.INST.failureStatus();
		case IN_PROGRESS:
			return JobTexts.INST.inProgressStatus();
		default:
		case SUCCESS:
			return JobTexts.INST.successStatus();
		}
	}
}
