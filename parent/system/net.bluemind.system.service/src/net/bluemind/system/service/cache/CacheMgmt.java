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
package net.bluemind.system.service.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ICacheMgmt;

public class CacheMgmt implements ICacheMgmt {
	private static final Logger logger = LoggerFactory.getLogger(CacheMgmt.class);
	private BmContext context;
	private RBACManager rbac;

	public CacheMgmt(BmContext context) {
		this.context = context;
		rbac = new RBACManager(context);
	}

	@Override
	public void flushCaches() throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGE_SYSTEM_CONF);
		OOPMessage hqMsg = MQ.newMessage();
		MQ.getProducer(Topic.CACHE_FLUSH).send(hqMsg);
	}
}
