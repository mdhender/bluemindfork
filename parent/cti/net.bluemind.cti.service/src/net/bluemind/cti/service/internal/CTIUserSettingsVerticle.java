/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cti.service.internal;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.service.CTIDeferredAction;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.user.api.IUserSettings;

public class CTIUserSettingsVerticle extends AbstractVerticle {

	private EventBus eb;

	@Override
	public void start() {
		this.eb = vertx.eventBus();
		registerSettingsHandler();
	}

	private void registerSettingsHandler() {
		eb.consumer("usersettings.updated", this::checkUpdatedUserSettings);
	}

	private void checkUpdatedUserSettings(Message<JsonObject> msg) {
		String containerUid = msg.body().getString("containerUid");
		String userUid = msg.body().getString("itemUid");

		IDeferredAction deferredActionService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDeferredAction.class, IDeferredActionContainerUids.uidForDomain(containerUid));
		Map<String, String> settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, containerUid).get(userUid);

		List<ItemValue<DeferredAction>> storedTriggers = deferredActionService
				.getByReference(CTIDeferredAction.reference(userUid));
		if (hasCalPresenceChanged(settings, storedTriggers)) {
			storedTriggers.forEach(action -> deferredActionService.delete(action.uid));
			if (isCalPresenceSet(settings)) {
				DeferredAction deferredAction = new DeferredAction();
				deferredAction.executionDate = new Date(Instant.now().toEpochMilli());
				deferredAction.actionId = CTIDeferredAction.ACTION_ID;
				deferredAction.reference = CTIDeferredAction.reference(userUid);
				deferredActionService.create(UUID.randomUUID().toString(), deferredAction);
			}
		}

	}

	private boolean hasCalPresenceChanged(Map<String, String> settings,
			List<ItemValue<DeferredAction>> storedTriggers) {
		return isCalPresenceSet(settings) == storedTriggers.isEmpty();
	}

	private boolean isCalPresenceSet(Map<String, String> settings) {
		return !"false".equals(settings.get("cal_set_phone_presence"));
	}

	public static class CTIUserSettingsVerticleFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new CTIUserSettingsVerticle();
		}

	}

}
