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
package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.dto.CoreTok;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class TokenContinuousHook implements IStateListener {

	private static final Logger logger = LoggerFactory.getLogger(TokenContinuousHook.class);
	private SystemState cur;

	@Override
	public void stateChanged(SystemState newState) {
		if (newState == SystemState.CORE_STATE_RUNNING && newState != cur) {
			BaseContainerDescriptor instContainer = BaseContainerDescriptor.create(InstallationId.getIdentifier(),
					"inst name", "system", "installation", null, true);
			CoreTok token = CoreTok.of(Token.admin0());
			ItemValue<CoreTok> asItem = ItemValue.create("core_tok", token);
			asItem.internalId = -1;
			DefaultBackupStore.store().<CoreTok>forContainer(instContainer).store(asItem);
			logger.info("Pushed token {} to installation container", asItem);
		}
		cur = newState;
	}

}
