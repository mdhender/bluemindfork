/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.directory.hollow.datamodel.consumer.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer.Blob;
import com.netflix.hollow.api.consumer.HollowConsumer.RefreshListener;
import com.netflix.hollow.api.custom.HollowAPI;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

public class LoggingRefreshListener implements RefreshListener {

	private static final Logger logger = LoggerFactory.getLogger(LoggingRefreshListener.class);
	private final String prefix;

	public LoggingRefreshListener(String logPrefix) {
		this.prefix = logPrefix;
	}

	@Override
	public void refreshStarted(long currentVersion, long requestedVersion) {
		logger.info("[{}] refreshStarted {} => {}", prefix, currentVersion, requestedVersion);
	}

	@Override
	public void snapshotUpdateOccurred(HollowAPI api, HollowReadStateEngine stateEngine, long version)
			throws Exception {
		logger.info("[{}] snapshotUpdateOccurred with engine {} and version {}", prefix, stateEngine, version);
	}

	@Override
	public void deltaUpdateOccurred(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
		logger.info("[{}] deltaUpdateOccurred with engine {} and version {}", prefix, stateEngine, version);
	}

	@Override
	public void blobLoaded(Blob transition) {
		logger.info("[{}] blobLoaded {}", prefix, transition);
	}

	@Override
	public void refreshSuccessful(long beforeVersion, long afterVersion, long requestedVersion) {
		logger.info("[{}] refreshSuccessful before {}, after {}, requested {}", prefix, beforeVersion, afterVersion,
				requestedVersion);
	}

	@Override
	public void refreshFailed(long beforeVersion, long afterVersion, long requestedVersion, Throwable failureCause) {
		logger.error("[{}] refreshFailed before {}, after {}, requested {}", prefix, beforeVersion, afterVersion,
				requestedVersion, failureCause);
	}

}
