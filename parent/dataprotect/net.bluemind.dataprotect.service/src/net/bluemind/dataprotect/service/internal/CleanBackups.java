/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.tool.ToolConfig;
import net.bluemind.server.api.IServer;

public class CleanBackups {
	private static final Logger logger = LoggerFactory.getLogger(CleanBackups.class);

	private final IDPContext dpCtx;
	private final IDataProtect backupApi;
	private final IServer serverApi;

	public CleanBackups(IDPContext dpCtx) {
		this.dpCtx = dpCtx;
		this.backupApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDataProtect.class);
		this.serverApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");
	}

	public void execute() {
		cleanFileSystem();
		cleanDatabase();
	}

	private void cleanFileSystem() {
		logger.info("Cleaning backup filesystem data");
		List<PartGeneration> validParts = backupApi.getAvailableGenerations().stream() //
				.filter(g -> g.valid()) //
				.flatMap(gen -> gen.parts.stream()) //
				.collect(Collectors.toList());

		List<Integer> validIds = validParts.stream() //
				.map(p -> p.id) //
				.collect(Collectors.toList());

		validParts.stream() //
				.map(p -> p.server) //
				.collect(Collectors.toSet()).stream() //
				.map(s -> serverApi.getComplete(s)).filter(srv -> srv != null && srv.value != null).forEach(srv -> {
					logger.info("Cleaning backups of server {}", srv.value.address());
					dpCtx.tool().newSession(new ToolConfig(srv, null, null)).clean(validIds);
				});
	}

	private void cleanDatabase() {
		logger.info("Cleaning backup database data");
		backupApi.getAvailableGenerations().stream().filter(g -> !g.valid()).forEach(this::forgetGeneration);
	}

	private void forgetGeneration(DataProtectGeneration dpg) {
		TaskRef ref = backupApi.forget(dpg.id);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), ref);
	}
}
