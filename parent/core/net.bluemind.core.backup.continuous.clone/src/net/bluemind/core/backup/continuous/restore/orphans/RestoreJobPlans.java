/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.Job;

public class RestoreJobPlans {

	private static final ValueReader<ItemValue<Job>> scReader = JsonUtils.reader(new TypeReference<ItemValue<Job>>() {
	});

	private static final Logger logger = LoggerFactory.getLogger(RestoreJobPlans.class);
	private final IJob jobApi;

	public RestoreJobPlans(IServiceProvider target) {
		this.jobApi = target.instance(IJob.class);
	}

	public void restore(IServerTaskMonitor monitor, List<DataElement> jobs) {

		for (DataElement d : jobs) {
			Job plan = scReader.read(d.payload).value;
			monitor.log("Restore plan for " + plan.id);
			jobApi.update(plan);
		}
	}

}
