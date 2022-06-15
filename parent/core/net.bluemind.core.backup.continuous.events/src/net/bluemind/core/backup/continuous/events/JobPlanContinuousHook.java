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
package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.scheduledjob.api.IJobHook;
import net.bluemind.scheduledjob.api.Job;

public class JobPlanContinuousHook implements IJobHook {

	private static final Logger logger = LoggerFactory.getLogger(JobPlanContinuousHook.class);

	public static class JobContainerAdapter implements ContinuousContenairization<Job> {

		private final IBackupStoreFactory tgt;

		public JobContainerAdapter(IBackupStoreFactory tgt) {
			this.tgt = tgt;
		}

		public JobContainerAdapter() {
			this(DefaultBackupStore.store());
		}

		@Override
		public String type() {
			return "job_plans";
		}

		@Override
		public IBackupStoreFactory targetStore() {
			return tgt;
		}

	}

	private final JobContainerAdapter jca = new JobContainerAdapter();

	@Override
	public void onJobUpdated(Job newVersion) {
		jca.save(null, "system", newVersion.id, newVersion, false);
		logger.info("Continuous saving {} plan.", newVersion.id);
	}

}
