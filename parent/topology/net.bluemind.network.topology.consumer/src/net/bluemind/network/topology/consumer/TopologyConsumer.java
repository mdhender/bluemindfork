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
package net.bluemind.network.topology.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.utils.JsonUtils;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.dto.TopologyPayload;

public class TopologyConsumer implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(TopologyConsumer.class);

	@Override
	public void handle(OOPMessage msg) {
		try {
			TopologyPayload payload = JsonUtils.read(msg.toJson().encode(), TopologyPayload.class);
			Topology.update(payload.nodes);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
