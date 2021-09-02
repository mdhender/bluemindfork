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
package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiReplica;

public class RestoreMapiArtifacts implements RestoreDomainType {

	private static final Logger logger = LoggerFactory.getLogger(RestoreMapiArtifacts.class);

	private static final ValueReader<ItemValue<MapiReplica>> replReader = JsonUtils
			.reader(new TypeReference<ItemValue<MapiReplica>>() {
			});
	private static final ValueReader<ItemValue<MapiFolder>> folderReader = JsonUtils
			.reader(new TypeReference<ItemValue<MapiFolder>>() {
			});

	private final IServerTaskMonitor monitor;
	private ItemValue<Domain> domain;
	private IServiceProvider target;

	public RestoreMapiArtifacts(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "mapi_artifacts";
	}

	@Override
	public void restore(DataElement de) {
		switch (de.key.valueClass) {
		case "net.bluemind.exchange.mapi.api.MapiReplica":
			setupReplica(de);
			break;
		case "net.bluemind.exchange.mapi.api.MapiFolder":
			setupFolder(de);
			break;
		default:
			logger.warn("Not handled type {}", de.key.valueClass);
			break;
		}
	}

	private void setupReplica(DataElement de) {
		IMapiMailbox replApi = target.instance(IMapiMailbox.class, domain.uid, de.key.owner);
		ItemValue<MapiReplica> replicaItem = replReader.read(new String(de.payload));
		logger.info("Restore {}", replicaItem.value);
		replApi.create(replicaItem.value);
	}

	private void setupFolder(DataElement de) {
		IMapiFoldersMgmt foldersApi = target.instance(IMapiFoldersMgmt.class, domain.uid, de.key.owner);
		ItemValue<MapiFolder> folderItem = folderReader.read(new String(de.payload));
		logger.info("Restore {}", folderItem.value);
		foldersApi.store(folderItem.value);
	}

}
