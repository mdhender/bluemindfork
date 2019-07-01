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
package net.bluemind.cti.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class CTIActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		MQ.init(() -> {
			MQ.registerConsumer(Topic.IM_NOTIFICATIONS, new HornetQListener());
			MQ.registerProducer(Topic.XIVO_PHONE_STATUS);

		});

	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
