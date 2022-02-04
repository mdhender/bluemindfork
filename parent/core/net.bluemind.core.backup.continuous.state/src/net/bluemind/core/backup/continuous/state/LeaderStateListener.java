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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.state;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.InstallationWriteLeader;
import net.bluemind.core.backup.continuous.dto.Seppuku;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class LeaderStateListener implements IStateListener {

	private static final Logger logger = LoggerFactory.getLogger(LeaderStateListener.class);
	private SystemState cur;

	@Override
	public void stateChanged(SystemState newState) {
		if (newState != cur && newState == SystemState.CORE_STATE_DEMOTED) {
			demote();
		}
		cur = newState;
	}

	private void demote() {
		InstallationWriteLeader leadership = DefaultBackupStore.store().leadership();
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		preventCoreStart();
		if (leadership.isLeader()) {
			writeByeMessageToKafka(prov);
			leadership.releaseLeadership();
		} else {
			logger.warn("{} says we are not leaders", leadership);
		}
	}

	private void preventCoreStart() {
		try {
			Path path = Paths.get("/etc/bm/bm-core.disabled");
			Files.createFile(path);
			logger.info("Wrote ({}) /etc/bm/bm-core.disabled to prevent further starts.", path);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void writeByeMessageToKafka(ServerSideServiceProvider prov) {
		IDomains domApi = prov.instance(IDomains.class);
		ItemValue<Domain> userDom = domApi.all().stream().filter(d -> !d.value.global).findFirst().orElse(null);
		if (userDom == null) {
			logger.error("We did not find a single domain....");
			return;
		}
		logger.info("seppuku time, write Bye message for {}", userDom.uid);
		IContainers contApi = prov.instance(IContainers.class);
		ContainerDescriptor dirDesc = contApi.get(userDom.uid);
		logger.info("Dir is {}", dirDesc);
		IBackupStore<Seppuku> store = DefaultBackupStore.store().forContainer(dirDesc);
		Seppuku sep = new Seppuku();
		sep.byeTime = new Date();
		ItemValue<Seppuku> bye = ItemValue.create("seppuku", sep);
		bye.internalId = bye.uid.hashCode();
		bye.created = sep.byeTime;
		store.store(bye);
	}

}
