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
package net.bluemind.scheduledjob.api;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class JobExecution {

	public Date startDate;
	public Date endDate;
	public String domainName;
	public JobExitStatus status;
	public String jobId;
	public String execGroup;
	public int id;

	@Override
	public boolean equals(Object obj) {
		JobExecution je = (JobExecution) obj;
		return (id == je.id && startDate.equals(je.startDate) && endDate.equals(je.endDate)
				&& domainName.equals(je.domainName) && status == je.status && jobId.equals(je.jobId)
				&& execGroup.equals(je.execGroup));
	}
}
