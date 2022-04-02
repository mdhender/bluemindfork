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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.Collections;
import java.util.List;

import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;

public class OrphansSync {

	private final BmContext context;

	public OrphansSync(BmContext context) {
		this.context = context;
	}

	public List<ItemValue<Domain>> syncOrphans(IBackupStoreFactory store, IServerTaskMonitor mon) {

		VersionSort vs = new VersionSort();
		IContainers contApi = context.provider().instance(IContainers.class);

		ContainerDescriptor installationContainer = contApi.get(InstallationId.getIdentifier());
		IServer srvApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = srvApi.allComplete();
		Collections.sort(servers, vs);

		IDomains domApi = context.provider().instance(IDomains.class);
		List<ItemValue<Domain>> domains = domApi.all();

		IBackupStore<Server> srvBackup = store.forContainer(installationContainer);
		servers.forEach(srvBackup::store);

		Collections.sort(domains, vs);
		ContainerDescriptor domCont = contApi.get(DomainsContainerIdentifier.getIdentifier());
		IBackupStore<Domain> domBackup = store.forContainer(domCont);
		domains.forEach(dom -> {
			domBackup.store(dom);
			IDomainSettings setApi = context.provider().instance(IDomainSettings.class, dom.uid);
			IBackupStore<DomainSettings> domSettingsBackup = store.forContainer(domCont);
			DomainSettings ds = new DomainSettings(dom.uid, setApi.get());
			domSettingsBackup.store(ItemValue.create(dom, ds));
			mon.log("Stored " + dom.value.defaultAlias + " and its settings.");
		});

		ISystemConfiguration sysconf = context.provider().instance(ISystemConfiguration.class);
		BaseContainerDescriptor confCont = BaseContainerDescriptor.create("sysconf", "sysconf", "system", "sysconf",
				null, true);
		IBackupStore<SystemConf> confBackup = store.forContainer(confCont);
		ItemValue<SystemConf> scItem = ItemValue.create("sysconf", sysconf.getValues());
		scItem.internalId = scItem.uid.hashCode();
		confBackup.store(scItem);

		return domains;
	}

}
