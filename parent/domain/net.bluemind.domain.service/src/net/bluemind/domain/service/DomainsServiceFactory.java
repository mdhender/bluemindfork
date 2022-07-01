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
package net.bluemind.domain.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class DomainsServiceFactory extends DomainsServiceCommonFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IDomains> {

	public DomainsServiceFactory() {
		MQ.init(() -> MQ.registerProducer(Topic.SYSTEM_NOTIFICATIONS));
	}

	@Override
	public Class<IDomains> factoryClass() {
		return IDomains.class;
	}

	@Override
	public IDomains instance(BmContext context, String... params) throws ServerFault {
		return instanceImpl(context);
	}
}
