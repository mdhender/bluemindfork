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

package net.bluemind.dataprotect.worker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IBackupWorker;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.server.api.Server;

public abstract class DefaultWorker implements IBackupWorker {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean supportsTag(String tag) {
		return false;
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		logger.info("prepareDataDirs");
	}

	@Override
	public Set<String> getDataDirs() {
		logger.info("getDataDirs");
		return new HashSet<>();
	}

	@Override
	public void dataDirsSaved(IDPContext ctx, String tag, ItemValue<Server> backedUp) throws ServerFault {
		logger.info("dataDirsSaved");
	}

	@Override
	public void restore(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault {
		logger.warn("No restore capabilities");
	}

	@Override
	public void cleanup(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault {
		logger.warn("No cleanup capabilities");
	}

}
