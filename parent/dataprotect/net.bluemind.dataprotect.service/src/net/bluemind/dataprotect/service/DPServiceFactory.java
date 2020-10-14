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

package net.bluemind.dataprotect.service;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.service.internal.DPService;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class DPServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IDataProtect> {

	private static List<RestoreOperation> ops;
	private static List<IRestoreActionProvider> providers;
	private static final Logger logger = LoggerFactory.getLogger(DPServiceFactory.class);

	static {
		loadOps();
	}

	@Override
	public Class<IDataProtect> factoryClass() {
		return IDataProtect.class;
	}

	/**
	 * @return
	 */
	private static void loadOps() {
		RunnableExtensionLoader<IRestoreActionProvider> rel = new RunnableExtensionLoader<>();
		List<IRestoreActionProvider> actions = rel.loadExtensions("net.bluemind.dataprotect.service", "restoreaction",
				"restore_action", "impl");
		List<RestoreOperation> ret = new LinkedList<>();
		for (IRestoreActionProvider rap : actions) {
			List<RestoreOperation> opList = rap.operations();
			logger.info("loading rop for kind {}", rap.getClass().getName());
			for (RestoreOperation op : opList) {
				logger.info("Loaded op {}", op.identifier);
			}
			ret.addAll(opList);
		}
		ops = ret;
		providers = actions;
		logger.info("Loaded{} restore ops", ops.size());
	}

	@Override
	public IDataProtect instance(BmContext context, String... params) throws ServerFault {
		return new DPService(context, ops, providers);
	}

}
