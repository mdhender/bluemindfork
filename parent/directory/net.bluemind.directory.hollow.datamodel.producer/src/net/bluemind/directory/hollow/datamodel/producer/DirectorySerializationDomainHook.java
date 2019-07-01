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
package net.bluemind.directory.hollow.datamodel.producer;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.lib.vertx.VertxPlatform;

public class DirectorySerializationDomainHook extends DomainHookAdapter {

	public static final String DOMAIN_CHANGE_EVENT = "domain.ser.changed";

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		JsonObject msg = new JsonObject();
		msg.putString("domain", domain.uid);
		msg.putString("action", "create");
		VertxPlatform.eventBus().send(DOMAIN_CHANGE_EVENT, msg);
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		JsonObject msg = new JsonObject();
		msg.putString("domain", domain.uid);
		msg.putString("action", "delete");
		VertxPlatform.eventBus().send(DOMAIN_CHANGE_EVENT, msg);
	}

}
