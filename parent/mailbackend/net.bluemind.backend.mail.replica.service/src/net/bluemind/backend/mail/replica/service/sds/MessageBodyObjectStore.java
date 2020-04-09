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
package net.bluemind.backend.mail.replica.service.sds;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.service.sds.IObjectStoreReader.Factory;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class MessageBodyObjectStore {

	private static final Logger logger = LoggerFactory.getLogger(MessageBodyObjectStore.class);
	private final BmContext ctx;
	private final IObjectStoreReader objectStoreReader;

	private static final IObjectStoreReader NOOP_READER = new IObjectStoreReader() {

		@Override
		public boolean exist(String guid) {
			return false;
		}

		@Override
		public Path read(String guid) {
			return null;
		}

		@Override
		public Path[] mread(String... guids) {
			return new Path[0];
		}

		@Override
		public String toString() {
			return "NOOP_READER:" + super.toString();
		}

	};

	private static final Map<String, IObjectStoreReader.Factory> archiveKindToObjectStore = loadStores();

	public MessageBodyObjectStore(BmContext ctx, CyrusPartition partition) {
		this.ctx = ctx;
		if (logger.isDebugEnabled()) {
			logger.debug("Object store for {} and partition {}", this.ctx, partition);
		}

		ISystemConfiguration config = ctx.provider().instance(ISystemConfiguration.class);
		this.objectStoreReader = loadReader(config.getValues());
		if (logger.isDebugEnabled()) {
			logger.debug("Reading with {}", objectStoreReader);
		}
	}

	private static Map<String, Factory> loadStores() {
		RunnableExtensionLoader<IObjectStoreReader.Factory> rel = new RunnableExtensionLoader<>();
		List<Factory> factories = rel.loadExtensions("net.bluemind.backend.mail.replica.service", "objectstore",
				"reader", "impl");
		return factories.stream().collect(Collectors.toMap(IObjectStoreReader.Factory::handledObjectStoreKind, f -> f));
	}

	private IObjectStoreReader loadReader(SystemConf values) {
		String archiveKind = values.stringValue(SysConfKeys.archive_kind.name());
		return Optional.ofNullable(archiveKindToObjectStore.get(archiveKind)).map(f -> f.create(values))
				.orElse(NOOP_READER);
	}

	/**
	 * Returns a sub-collections containing only the GUIDs that our known in the
	 * object store
	 * 
	 * @param bodyGuid
	 * @return
	 */
	public Set<String> exist(Set<String> bodyGuid) {
		logger.debug("Checking with {}", objectStoreReader);
		return bodyGuid.stream().filter(objectStoreReader::exist).collect(Collectors.toSet());
	}

	public Path open(String guid) {
		logger.debug("Open {} with {}", guid, objectStoreReader);
		return objectStoreReader.read(guid);
	}

	public Path[] mopen(String[] guid) {
		logger.debug("Open {} with {}", guid, objectStoreReader);
		return objectStoreReader.mread(guid);
	}

}
