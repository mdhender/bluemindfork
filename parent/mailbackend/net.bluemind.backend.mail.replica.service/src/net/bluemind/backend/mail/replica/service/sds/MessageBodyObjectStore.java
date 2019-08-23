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

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.vertx.VertxStream;

public class MessageBodyObjectStore {

	private static final File sdsDir = new File("/dummy-sds");
	private static final Logger logger = LoggerFactory.getLogger(MessageBodyObjectStore.class);
	private final BmContext ctx;

	public MessageBodyObjectStore(BmContext ctx, CyrusPartition partition) {
		this.ctx = ctx;
		logger.debug("Object store for {} and partition {}", this.ctx, partition);
	}

	/**
	 * Returns a sub-collections containing only the GUIDs that our known in the
	 * object store
	 * 
	 * @param bodyGuid
	 * @return
	 */
	public Set<String> exist(Set<String> bodyGuid) {
		if (!sdsDir.isDirectory()) {
			return Collections.emptySet();
		}
		return bodyGuid.stream().filter(guid -> new File(sdsDir, guid).exists()).collect(Collectors.toSet());
	}

	public Stream open(String guid) {
		File f = new File(sdsDir, guid);
		if (!f.exists()) {
			throw ServerFault.notFound("Body " + guid + " (" + f.getAbsolutePath() + ") is missing from object store.");
		}
		return VertxStream.localPath(f.toPath());
	}

}
