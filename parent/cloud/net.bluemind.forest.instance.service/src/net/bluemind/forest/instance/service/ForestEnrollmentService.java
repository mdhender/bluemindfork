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
package net.bluemind.forest.instance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.forest.instance.api.ForestEnpoints;
import net.bluemind.forest.instance.api.IForestEnrollment;

public class ForestEnrollmentService implements IForestEnrollment {

	private static final Logger logger = LoggerFactory.getLogger(ForestEnrollmentService.class);

	private final BmContext context;

	public ForestEnrollmentService(BmContext context) {
		this.context = context;
		logger.debug("{}", this.context);
	}

	@Override
	public void checkpoint(ForestEnpoints endpoints) {
		logger.info("Checkpoint {}", endpoints);
	}

	public static class FEnrollServiceFactory
			implements ServerSideServiceProvider.IServerSideServiceFactory<IForestEnrollment> {

		@Override
		public Class<IForestEnrollment> factoryClass() {
			return IForestEnrollment.class;
		}

		@Override
		public IForestEnrollment instance(BmContext context, String... params) throws ServerFault {
			if (params == null || params.length != 0) {
				throw new ServerFault("wrong number of instance parameters");
			}

			return new ForestEnrollmentService(context);
		}

	}

}
