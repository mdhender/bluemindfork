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

package net.bluemind.dataprotect.service.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.BackupPath;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ForgetTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(ForgetTask.class);
	private final DataProtectGeneration gen;
	private final BmContext ctx;
	private DPService dpApi;

	public ForgetTask(BmContext ctx, DPService dpApi, DataProtectGeneration gen) {
		this.gen = gen;
		this.ctx = ctx;
		this.dpApi = dpApi;
		logger.debug("{}", this.ctx);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(1, "forgetting generation " + gen.id + "...");

		List<DataProtectGeneration> allGens = dpApi.getAvailableGenerations();
		DataProtectGeneration toForget = null;
		for (DataProtectGeneration g : allGens) {
			if (gen.id == g.id) {
				toForget = g;
				break;
			}
		}

		if (toForget == null) {
			monitor.end(true, "forgotten", "{ \"status\": \"not_found\" }");
			return;
		}

		monitor.subWork("part_iteration", toForget.parts.size());
		IServer srvApi = ctx.getServiceProvider().instance(IServer.class, InstallationId.getIdentifier());
		for (PartGeneration pg : toForget.parts) {
			monitor.progress(1, "on part " + pg.server + " / " + pg.tag);
			ItemValue<Server> srv = srvApi.getComplete(pg.server);
			if (srv != null && srv.value != null) {
				INodeClient nc = NodeActivator.get(srv.value.address());
				String path = BackupPath.get(srv, pg.tag) + "/" + pg.id;
				monitor.log("Removing " + path);
				String empty = "/tmp/empty" + UUID.randomUUID().toString();
				NCUtils.execNoOut(nc, "mkdir -p " + empty);
				// run this one over websocket to avoid bm-node polling
				NCUtils.execNoOut(nc, "/usr/bin/rsync -a --delete " + empty + "/ " + path + "/", 8, TimeUnit.HOURS);
				NCUtils.execNoOut(nc, "rm -fr " + path);
				NCUtils.execNoOut(nc, "rmdir " + empty);
			} else {
				monitor.log("Skip removing " + pg.server + " because does not exist anymore");
				logger.info("Skip removing {} because does not exist anymore", pg.server);
			}
		}

		List<DataProtectGeneration> updated = new LinkedList<>(allGens);
		updated.remove(gen);
		dpApi.getStore().rewriteGenerations(updated);

		// FIXME remove in backup too

		monitor.end(true, "forgotten", "{ \"status\": \"ok\" }");
	}
}
