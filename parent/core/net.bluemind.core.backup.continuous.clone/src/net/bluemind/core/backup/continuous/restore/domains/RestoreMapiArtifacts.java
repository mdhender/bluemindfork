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

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
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

	private final RestoreLogger log;
	private ItemValue<Domain> domain;
	private IServiceProvider target;

	public RestoreMapiArtifacts(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		this.log = log;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "mapi_artifacts";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		switch (key.valueClass) {
		case "net.bluemind.exchange.mapi.api.MapiReplica":
			setupReplica(key, payload);
			break;
		case "net.bluemind.exchange.mapi.api.MapiFolder":
			setupFolder(key, payload);
			break;
		default:
			log.skip(type(), key, payload);
			break;
		}
	}

	private void setupReplica(RecordKey key, String payload) {
		IMapiMailbox replApi = target.instance(IMapiMailbox.class, domain.uid, key.owner);
		ItemValue<MapiReplica> replicaItem = replReader.read(payload);
		log.create(type(), "replica", key);
		replApi.create(replicaItem.value);
	}

	private void setupFolder(RecordKey key, String payload) {
		IMapiMailbox replApi = target.instance(IMapiMailbox.class, domain.uid, key.owner);
		MapiReplica replica = replApi.get();
		if (replica == null) {
			log.filter(type(), "folder", key);
			return;
		}
		log.create(type(), "folder", key);
		IMapiFoldersMgmt foldersApi = target.instance(IMapiFoldersMgmt.class, domain.uid, key.owner);
		ItemValue<MapiFolder> folderItem = folderReader.read(payload);
		foldersApi.store(folderItem.value);
	}

}
